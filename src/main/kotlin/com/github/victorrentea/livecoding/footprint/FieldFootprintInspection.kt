package com.github.victorrentea.livecoding.footprint

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTypesUtil

/**
 * Cheap, on-the-fly inspection: flags a fat-object parameter (WEAK_WARNING) when it is used as a
 * thin DTO — read through only a few getters and never propagated out of the method.
 *
 * PERFORMANCE (the whole reason this is a separate tier from the intention): it runs
 * [FieldFootprintAnalyzer] at `maxDepth = 0`, which never resolves a callee (no cross-file
 * `resolveMethod`). The instant the parameter is passed onward / stored / serialized, the
 * analysis returns non-SOUND and we skip the highlight. So the per-parameter cost is bounded by
 * the current method body — file-local, comparable to the plugin's other on-the-fly inspections,
 * and free of the global cache-invalidation problem that makes a *transitive* inspection
 * infeasible. The full transitive footprint stays in the on-demand "Annotate field footprint"
 * intention (Alt+Enter), which is also offered on the same parameter.
 */
class FieldFootprintInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitParameter(parameter: PsiParameter) {
                val method = parameter.declarationScope as? PsiMethod ?: return
                if (method.body == null) return

                val psiClass = PsiTypesUtil.getPsiClass(parameter.type) ?: return
                if (!FootprintTargets.isTarget(psiClass)) return
                val fqn = psiClass.qualifiedName ?: return

                // maxDepth = 0 => strictly intra-procedural, no cross-file resolution.
                // SOUND here means the parameter never escapes, so this local read set is complete.
                val fp = FieldFootprintAnalyzer(fqn, maxDepth = 0).analyzeParameter(method, parameter)
                if (fp.verdict != Verdict.SOUND || fp.paths.isEmpty()) return

                val fieldCount = FootprintTargets.instanceFieldCount(psiClass)
                if (fp.paths.size > thinThreshold(fieldCount)) return

                holder.registerProblem(
                    parameter.nameIdentifier ?: parameter,
                    "'${psiClass.name}' parameter reads only " +
                        fp.paths.sorted().joinToString(", ", "{", "}") +
                        " of $fieldCount fields — thin-DTO candidate",
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
        }

    /** Thin when reads <= 3, or <= 20% of the class's fields (whichever is larger). */
    private fun thinThreshold(fieldCount: Int): Int = maxOf(THIN_ABSOLUTE, fieldCount / THIN_FRACTION_DENOM)

    companion object {
        private const val THIN_ABSOLUTE = 3
        private const val THIN_FRACTION_DENOM = 5
    }
}
