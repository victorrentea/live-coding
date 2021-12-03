package com.github.victorrentea.livecoding

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class Slf4jAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!FrameworkDetector.lombokIsPresent(element.project, element)) return

        if (element is PsiExpressionList) {
            val callExpression = element.parent as? PsiMethodCallExpression ?: return
            val reference = callExpression.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return
            if (reference.text == "log" && reference.resolve() == null) {
                addAnnotation(holder, element)
            }
        }

        if (element is PsiReferenceExpression &&
            element.text == "log" &&
            element.resolve() == null // no 'log' is defined in the context
        ) {
            addAnnotation(holder, element)
        }
    }

    private fun addAnnotation(holder: AnnotationHolder, element: PsiElement) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Add @Slf4j to class (lombok)")
            .highlightType(ProblemHighlightType.GENERIC_ERROR)
            .withFix(AddSlf4jAnnotationQuickFix(element))
            .create()
    }
}

data class AddSlf4jAnnotationQuickFix(val logExpression: PsiElement) : BaseIntentionAction() {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Add @Slf4j to class (lombok)"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val parentClass = PsiTreeUtil.getParentOfType(logExpression, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return
        if (modifiers.hasAnnotation("lombok.extern.slf4j.Slf4j")) return  // no lombok plugin ?
        val annotation = modifiers.addAnnotation("lombok.extern.slf4j.Slf4j")
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
    }
}

