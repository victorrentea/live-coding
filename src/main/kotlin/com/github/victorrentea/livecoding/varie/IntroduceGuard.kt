package com.github.victorrentea.livecoding.varie

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiIfStatement


class IntroduceGuardInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return IntroduceGuardVisitor(holder)
    }

}

class IntroduceGuardVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        val psiIf = element as? PsiIfStatement ?: return

        holder.registerProblem(
            psiIf.firstChild,
            "The alternate (else) branch is much lighter\nWhen the else branch of an IF is trivial, consider flipping the if to start with the anemic branch and early return to keep the method more flat",
            ProblemHighlightType.WARNING
        )
    }
}

