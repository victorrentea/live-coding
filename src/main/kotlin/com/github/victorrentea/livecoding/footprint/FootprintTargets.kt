package com.github.victorrentea.livecoding.footprint

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTypesUtil

/**
 * Decides which parameter types are worth an on-demand field footprint.
 *
 * General-purpose and project-agnostic: a class qualifies as a "fat object" (a thin-DTO
 * candidate) when it declares at least [minInstanceFields] instance fields. No class names or
 * packages are hard-coded. [forcedFqns] is an optional user opt-in for specific classes; a
 * Settings page to edit both is the natural next step.
 */
object FootprintTargets {

    /** A class with at least this many instance fields is considered a fat object. */
    var minInstanceFields: Int = 8

    /** Optional explicit opt-in classes, regardless of field count. Empty by default. */
    var forcedFqns: Set<String> = emptySet()

    fun isTarget(type: PsiType?): Boolean {
        val psiClass = PsiTypesUtil.getPsiClass(type) ?: return false
        return isTarget(psiClass)
    }

    fun isTarget(psiClass: PsiClass): Boolean {
        psiClass.qualifiedName?.let { if (it in forcedFqns) return true }
        if (psiClass.isInterface || psiClass.isEnum || psiClass.isAnnotationType) return false
        val instanceFields = psiClass.allFields.count { !it.hasModifierProperty(PsiModifier.STATIC) }
        return instanceFields >= minInstanceFields
    }
}

/** True if [type] resolves to one of [fqns] (or a subclass of one) — matches one *specific* target type. */
fun isTargetType(type: PsiType?, fqns: Set<String>): Boolean {
    if (type == null) return false
    val psiClass = PsiTypesUtil.getPsiClass(type) ?: return false
    val qn = psiClass.qualifiedName
    if (qn != null && qn in fqns) return true
    return fqns.any { InheritanceUtil.isInheritor(psiClass, it) }
}
