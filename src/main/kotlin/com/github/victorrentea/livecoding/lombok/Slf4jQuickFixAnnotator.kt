package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.FrameworkDetector
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil


class Slf4jQuickFixAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
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
        if (FrameworkDetector.lombokIsPresent(element) && FrameworkDetector.slf4jIsPresent(element)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Add @Slf4j on class (lombok)")
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .withFix(AddSlf4jAnnotationQuickFix(element))
                .create()
        }
        if (FrameworkDetector.slf4jIsPresent(element)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Add a Slf4j 'log' field on the class")
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .withFix(AddSlf4jFieldQuickFix(element))
                .create()
        }
    }

    class AddSlf4jAnnotationQuickFix(val logExpression: PsiElement) : BaseIntentionAction() {
        override fun getFamilyName() = "Live-Coding"

        override fun getText() = "Add @Slf4j on class (lombok)"

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            val parentClass = PsiTreeUtil.getParentOfType(logExpression, PsiClass::class.java) ?: return
            val modifiers = parentClass.modifierList ?: return
            if (modifiers.hasAnnotation("lombok.extern.slf4j.Slf4j")) return  // no lombok plugin ?

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project, "Add @Slf4j on Class", "Live-Coding", {
                    val annotation = modifiers.addAnnotation("lombok.extern.slf4j.Slf4j")
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
                })
            }
        }
    }

    class AddSlf4jFieldQuickFix(val logExpression: PsiElement) : BaseIntentionAction() {
        override fun getFamilyName() = "Live-Coding"

        override fun getText() = "Add a Slf4j 'Logger log' field on the class"

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            val parentClass = PsiTreeUtil.getParentOfType(logExpression, PsiClass::class.java) ?: return
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project, "Add Slf4j Logger on Class", "Live-Coding", {
                    val factory = JavaPsiFacade.getInstance(project).elementFactory
                    val logField = factory.createFieldFromText(
                        "private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(${parentClass.name}.class);",
                        parentClass
                    );
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(parentClass.add(logField))
                })
            }
        }
    }
}


