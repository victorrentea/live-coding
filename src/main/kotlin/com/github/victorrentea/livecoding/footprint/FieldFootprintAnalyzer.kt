package com.github.victorrentea.livecoding.footprint

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiTreeUtil

/**
 * Field-sensitive, inter-procedural (transitive) collector of the getter/setter "footprint" of
 * a fat object across a method's call graph.
 *
 * Per-method local footprint (fields read/written on the target) is unioned with the footprint
 * of every callee that receives the target, taken to a fixpoint with a visited set for
 * recursion and a depth bound. Whole-object sinks short-circuit to [Verdict.WHOLE_OBJECT];
 * opaque boundaries degrade to [Verdict.UNKNOWN] rather than lying.
 *
 * One instance analyzes one specific target type ([targetFqn], the analyzed parameter's class).
 * EXPENSIVE and inter-procedural: run on demand (intention / batch), never in an on-the-fly
 * inspection. Must be invoked inside a read action.
 */
class FieldFootprintAnalyzer(
    private val targetFqn: String,
    private val maxDepth: Int = 15,
) {
    private val visited = HashSet<String>()

    /** Footprint for a single target-typed parameter [param] of [entry]. */
    fun analyzeParameter(entry: PsiMethod, param: PsiParameter): Footprint {
        visited.clear()
        return analyzeMethod(entry, setOf(param), 0)
    }

    private fun analyzeMethod(method: PsiMethod, targetVars: Set<PsiVariable>, depth: Int): Footprint {
        ProgressManager.checkCanceled()
        val body = method.body ?: return Footprint.unknown("No source for ${method.name}()")
        if (depth > maxDepth) return Footprint.unknown("Max depth reached at ${method.name}()")

        if (!visited.add(keyOf(method, targetVars))) return Footprint.EMPTY // cycle

        val tracked = HashSet(targetVars)
        expandAliases(body, tracked)

        var result = Footprint.EMPTY

        for (ref in PsiTreeUtil.collectElementsOfType(body, PsiReferenceExpression::class.java)) {
            if (ref.qualifierExpression != null) continue // only plain `x`, not `x.y`
            val resolved = ref.resolve()
            if (resolved is PsiVariable && resolved in tracked) {
                result = merge(result, classify(ref, method, depth))
                if (result.verdict == Verdict.WHOLE_OBJECT) return result
            }
        }

        // Casts to the target type: `((Target) x).getFoo()`.
        for (cast in PsiTreeUtil.collectElementsOfType(body, PsiTypeCastExpression::class.java)) {
            if (matches(cast.type)) {
                result = merge(result, classify(cast, method, depth))
                if (result.verdict == Verdict.WHOLE_OBJECT) return result
            }
        }
        return result
    }

    private fun classify(occurrence: PsiExpression, method: PsiMethod, depth: Int): Footprint {
        var host: PsiElement = occurrence
        while (host.parent is PsiParenthesizedExpression) host = host.parent!!
        return when (val parent = host.parent) {
            // Bound method reference `target::getFoo` / `target::setBar` (a PsiMethodReferenceExpression
            // is itself a PsiReferenceExpression, so this branch MUST precede the generic one below).
            // The method is not invoked here, so there is no syntactic chain to follow.
            is PsiMethodReferenceExpression ->
                if (parent.qualifierExpression === host) methodRefOnTarget(parent.referenceName) else Footprint.EMPTY
            is PsiReferenceExpression -> {
                if (parent.qualifierExpression === host) {
                    val call = parent.parent as? PsiMethodCallExpression ?: return Footprint.EMPTY
                    callOnTarget(parent, call)
                } else {
                    Footprint.EMPTY
                }
            }
            is PsiExpressionList -> passedAsArgument(host as PsiExpression, parent, parent.parent, depth)
            is PsiTypeCastExpression ->
                if (matches(parent.type)) classify(parent, method, depth) else Footprint.EMPTY
            is PsiAssignmentExpression -> {
                val lhs = parent.lExpression
                when {
                    lhs is PsiReferenceExpression && lhs.resolve() is PsiField ->
                        Footprint.unknown("Stored into field ${lhs.referenceName} in ${method.name}() (escapes)")
                    lhs is PsiArrayAccessExpression ->
                        Footprint.unknown("Stored into an array element in ${method.name}() (escapes)")
                    else -> Footprint.EMPTY // reassignment `y = target` is tracked via expandAliases()
                }
            }
            // `"" + target` (string concatenation) implicitly calls target.toString() -> reads unknown fields.
            is PsiPolyadicExpression ->
                if (parent.operationTokenType == JavaTokenType.PLUS) {
                    Footprint.unknown("Implicit toString() via string concatenation in ${method.name}()")
                } else {
                    Footprint.EMPTY
                }
            is PsiReturnStatement -> Footprint.unknown("Returned from ${method.name}() (escapes)")
            else -> Footprint.EMPTY // null-checks, ==, instanceof, ... : no field access
        }
    }

    private fun callOnTarget(ref: PsiReferenceExpression, call: PsiMethodCallExpression): Footprint {
        val name = ref.referenceName ?: return Footprint.unknown("Unresolved call on target")
        if (name in Sinks.WHOLE_OBJECT_METHODS) {
            return Footprint.wholeObject("Calls target.$name() (reads whole object)")
        }
        getterField(name)?.let { field -> return Footprint.sound(setOf(chainedPath(call, field))) }
        setterField(name)?.let { field -> return Footprint.written(field) }
        return Footprint.unknown("Calls non-getter target.$name() (internal reads not analyzed)")
    }

    /**
     * Classify a bound method reference `target::name` (e.g. `stream.map(order::getId)` or
     * `list.forEach(order::setStatus)`). Same getter/setter/whole-object rules as a direct call,
     * but a method reference can never be chained, so the read is always a single field.
     */
    private fun methodRefOnTarget(name: String?): Footprint {
        val n = name ?: return Footprint.unknown("Unresolved method-ref on target")
        if (n in Sinks.WHOLE_OBJECT_METHODS) return Footprint.wholeObject("Method-ref target::$n (reads whole object)")
        getterField(n)?.let { field -> return Footprint.sound(setOf(field)) }
        setterField(n)?.let { field -> return Footprint.written(field) }
        return Footprint.unknown("Method-ref target::$n (internal reads not analyzed)")
    }

    private fun passedAsArgument(
        host: PsiExpression,
        argList: PsiExpressionList,
        call: PsiElement?,
        depth: Int,
    ): Footprint {
        val index = argList.expressions.indexOfFirst { it === host }
        if (index < 0) return Footprint.EMPTY
        val callExpr = call as? PsiCallExpression ?: return Footprint.unknown("Target used in a non-call expression")

        sinkForCall(callExpr)?.let { return it }

        val callee = callExpr.resolveMethod() ?: return Footprint.unknown("Target passed to an unresolved method")
        if (callee.body == null) return Footprint.unknown("Target passed to opaque ${callee.name}() (no source)")

        val params = callee.parameterList.parameters
        if (index >= params.size) return Footprint.unknown("Target passed to vararg/mismatched ${callee.name}()")

        val calleeParam = params[index]
        if (!matches(calleeParam.type)) {
            return Footprint.unknown(
                "Target widened to ${calleeParam.type.presentableText} in ${callee.name}() (may be cast & read)",
            )
        }
        return analyzeMethod(callee, setOf(calleeParam), depth + 1)
    }

    private fun sinkForCall(call: PsiCallExpression): Footprint? {
        if (call !is PsiMethodCallExpression) return null
        val name = call.methodExpression.referenceName ?: return null
        if (name !in Sinks.SINK_METHODS) return null
        val qualifier = call.methodExpression.qualifierExpression
        val hint = (qualifier?.type?.presentableText ?: "") + " " + (qualifier?.text ?: "")
        if (Sinks.SINK_QUALIFIER_TYPES.any { hint.contains(it) }) {
            return Footprint.wholeObject("Target reaches $hint.$name() (whole-object serialization/scripting)")
        }
        return null
    }

    /** Extend `target.getA()` outward through `.getB().getC()` into a dotted path "a.b.c". */
    private fun chainedPath(firstCall: PsiMethodCallExpression, firstField: String): String {
        val parts = mutableListOf(firstField)
        var current: PsiExpression = firstCall
        var guard = 0
        while (guard++ < 20) {
            val next = outerGetterCall(current) ?: break
            val field = getterField(next.methodExpression.referenceName ?: "") ?: break
            parts.add(field)
            current = next
        }
        return parts.joinToString(".")
    }

    private fun outerGetterCall(expr: PsiExpression): PsiMethodCallExpression? {
        var host: PsiElement = expr
        while (host.parent is PsiParenthesizedExpression) host = host.parent!!
        val ref = host.parent as? PsiReferenceExpression ?: return null
        if (ref.qualifierExpression !== host) return null
        val call = ref.parent as? PsiMethodCallExpression ?: return null
        return if (getterField(ref.referenceName ?: "") != null) call else null
    }

    private fun expandAliases(body: PsiCodeBlock, tracked: MutableSet<PsiVariable>) {
        var changed = true
        var guard = 0
        while (changed && guard++ < 6) {
            changed = false
            // Local declared and initialized from the target: `T y = target;`
            for (local in PsiTreeUtil.collectElementsOfType(body, PsiLocalVariable::class.java)) {
                if (local in tracked) continue
                val init = local.initializer ?: continue
                if (unwrapsToTracked(init, tracked) && tracked.add(local)) changed = true
            }
            // Local reassigned from the target: `y = target;` (flow-insensitive, conservative superset).
            for (assign in PsiTreeUtil.collectElementsOfType(body, PsiAssignmentExpression::class.java)) {
                val v = (assign.lExpression as? PsiReferenceExpression)?.resolve()
                if (v !is PsiVariable || v is PsiField || v in tracked) continue
                val rhs = assign.rExpression ?: continue
                if (unwrapsToTracked(rhs, tracked) && tracked.add(v)) changed = true
            }
        }
    }

    private fun unwrapsToTracked(expr: PsiExpression, tracked: Set<PsiVariable>): Boolean {
        var e: PsiExpression? = expr
        while (true) {
            when (e) {
                is PsiParenthesizedExpression -> e = e.expression
                is PsiTypeCastExpression -> e = e.operand
                is PsiReferenceExpression -> {
                    if (e.qualifierExpression != null) return false
                    val r = e.resolve()
                    return r is PsiVariable && r in tracked
                }
                else -> return false
            }
        }
    }

    private fun getterField(name: String): String? = when {
        name.length > 3 && name.startsWith("get") && name[3].isUpperCase() -> jbDecapitalize(name.substring(3))
        name.length > 2 && name.startsWith("is") && name[2].isUpperCase() -> jbDecapitalize(name.substring(2))
        else -> null
    }

    private fun setterField(name: String): String? =
        if (name.length > 3 && name.startsWith("set") && name[3].isUpperCase()) jbDecapitalize(name.substring(3)) else null

    /** JavaBeans property name: a leading run of capitals is kept (getURL -> "URL"; getId -> "id"). */
    private fun jbDecapitalize(s: String): String =
        if (s.length > 1 && s[0].isUpperCase() && s[1].isUpperCase()) s
        else s.replaceFirstChar { it.lowercaseChar() }

    private fun matches(type: PsiType?): Boolean = isTargetType(type, setOf(targetFqn))

    private fun keyOf(method: PsiMethod, targetVars: Set<PsiVariable>): String {
        val cls = method.containingClass?.qualifiedName ?: "?"
        val vars = targetVars.mapNotNull { it.name }.sorted().joinToString(",")
        return "$cls#${method.name}/${method.parameterList.parametersCount}::$vars"
    }
}
