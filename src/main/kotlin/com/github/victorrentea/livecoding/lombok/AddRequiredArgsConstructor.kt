package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.lombok.FrameworkDetector.lombokIsPresent
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil


class AddRequiredArgsConstructorInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!lombokIsPresent(holder.project, holder.file))
            return PsiElementVisitor.EMPTY_VISITOR

        return AddRequiredArgsConstructorVisitor(holder)
    }
}

class AddRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(field: PsiElement) {
        if (field is PsiField &&
            field.hasModifierProperty(PsiModifier.FINAL) &&
            !field.hasModifierProperty(PsiModifier.STATIC) &&
            field.containingClass?.constructors?.isEmpty() == true) {

            holder.registerProblem(
                field,
                "Final field(s) can be injected via @RequiredArgsConstructor",
                ProblemHighlightType.GENERIC_ERROR, // red underline
                field.nameIdentifier.textRangeInParent.grown(1), // +1 so ALT-ENTER works even after ;
                AddRequiredArgsConstructorQuickFix(field)
            )
        }

    }

}

class AddRequiredArgsConstructorQuickFix(field: PsiField) : LocalQuickFixOnPsiElement(field) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Add @RequiredArgsConstructor (lombok)"

    override fun invoke(project: Project, file: PsiFile, constructor: PsiElement, endElement: PsiElement) {
        val parentClass = PsiTreeUtil.getTopmostParentOfType(startElement, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return
        val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
    }

}