package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.FrameworkDetector.lombokIsPresent
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.min


class AddRequiredArgsConstructorInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!lombokIsPresent(holder.project, holder.file))
            return PsiElementVisitor.EMPTY_VISITOR

        return AddRequiredArgsConstructorVisitor(holder)
    }
}

class AddRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(field: PsiElement) {
        if (field !is PsiField) return

        if (field.hasModifierProperty(PsiModifier.FINAL) &&
            !field.hasModifierProperty(PsiModifier.STATIC) &&
            !field.hasInitializer() &&
            field.containingClass?.constructors?.isEmpty() == true) {

            val textLength = min(
                field.nameIdentifier.textRangeInParent.endOffset + 1,
                field.nameIdentifier.parent.textRange.length)
            val textRange = TextRange(0, textLength)  // +1 so ALT-ENTER works even after ;

            holder.registerProblem(
                field,
                "Final field(s) can be injected via @RequiredArgsConstructor",
                ProblemHighlightType.GENERIC_ERROR, // red underline
                textRange,
                AddRequiredArgsConstructorQuickFix(field)
            )
        }

    }

}

class AddRequiredArgsConstructorQuickFix(field: PsiField) : LocalQuickFixOnPsiElement(field) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Add @RequiredArgsConstructor (lombok)"

    override fun invoke(project: Project, file: PsiFile, constructor: PsiElement, endElement: PsiElement) {
        val parentClass = PsiTreeUtil.getParentOfType(startElement, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return
        val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
    }

}