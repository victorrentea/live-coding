package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.lombok.FrameworkDetector.visibleForTestingIsPresent
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.*


class VisibleForTestingAbuseInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!visibleForTestingIsPresent(holder.project, holder.file))
            return PsiElementVisitor.EMPTY_VISITOR

        return VisibleForTestingAbuseVisitor(holder)
    }
}

class VisibleForTestingAbuseVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(call: PsiElement) {
        if (call !is PsiCall) return
        if (call.resolveMethod() == null) return
        if (!call.resolveMethod()!!.hasAnnotation("com.google.common.annotations.VisibleForTesting")) return
        if (TestSourcesFilter.isTestSources(call.containingFile.virtualFile, call.project)) return

        holder.registerProblem(
            call,
            "Illegal call to @VisibleForTesting (guava) from production code",
            ProblemHighlightType.GENERIC_ERROR
        )

    }
}
