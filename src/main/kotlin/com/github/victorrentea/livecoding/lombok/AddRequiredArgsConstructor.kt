package com.github.victorrentea.livecoding.lombok

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil


class AddRequiredArgsConstructorInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return AddRequiredArgsConstructorVisitor(holder)
    }
}

class AddRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(field: PsiElement) {
        JavaPsiFacade.getInstance(holder.project)
            .findClass("lombok.RequiredArgsConstructor", field.resolveScope) ?: return

        if (field is PsiField &&
            field.hasModifier(JvmModifier.FINAL) &&
            field.containingClass!!.constructors.isEmpty()) {

            holder.registerProblem(
                field, "Final fields can be injected via @RequiredArgsConstructor",
                ProblemHighlightType.ERROR,
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