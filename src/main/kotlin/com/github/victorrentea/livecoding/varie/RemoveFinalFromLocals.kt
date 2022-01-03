package com.github.victorrentea.livecoding.varie

import com.github.victorrentea.livecoding.lombok.ReplaceRequiredArgsConstructorInspection.Companion.INSPECTION_NAME
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*


class RemoveFinalFromLocalInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Excessive final keyword may clutter the code"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return RemoveFinalFromLocalVisitor(holder)
    }
}

class RemoveFinalFromLocalVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        if (
            element is PsiLocalVariable && element.modifierList?.hasModifierProperty(PsiModifier.FINAL) == true ||
            element is PsiParameter && element.modifierList?.hasModifierProperty(PsiModifier.FINAL) == true
        )
            holder.registerProblem(
                element,
                INSPECTION_NAME,
                ProblemHighlightType.INFORMATION,
                RemoveFinalFromLocalsFix(element as PsiModifierListOwner)
            )
    }
}

class RemoveFinalFromLocalsFix(finalElement: PsiModifierListOwner) : LocalQuickFixOnPsiElement(finalElement) {
    companion object {
        const val FIX_NAME = "Remove final modifier"
    }

    override fun getFamilyName() = "Live-Coding"

    override fun getText() = FIX_NAME

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val finalElement = startElement as? PsiModifierListOwner ?: return
        finalElement.modifierList?.setModifierProperty(PsiModifier.FINAL, false);
    }

}

