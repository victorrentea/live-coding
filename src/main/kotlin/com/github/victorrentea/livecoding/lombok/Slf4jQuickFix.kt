package com.github.victorrentea.livecoding.lombok

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class Slf4jAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        JavaPsiFacade.getInstance(element.project)
            .findClass("lombok.extern.slf4j.Slf4j", element.resolveScope) ?: return

        if (element is PsiReferenceExpression &&
            element.text == "log" &&
            element.resolve() == null
        ) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Add @Slf4j to class (lombok)")
                .range(element.textRange)
                .tooltip("@Slf4j")
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .withFix(AddSlf4jAnnotationQuickFix(element))
                .create()
        }
    }
}

data class AddSlf4jAnnotationQuickFix(val logExpression: PsiReferenceExpression) : BaseIntentionAction() {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Add @Slf4j to class"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val parentClass = PsiTreeUtil.getTopmostParentOfType(logExpression, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return
        val annotation = modifiers.addAnnotation("lombok.extern.slf4j.Slf4j")
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
    }
}

