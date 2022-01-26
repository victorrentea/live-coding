package com.github.victorrentea.livecoding.varie

import com.github.victorrentea.livecoding.complexity.CognitiveComplexityVisitor
import com.intellij.codeInsight.intention.impl.InvertIfConditionAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiStatement
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix


class IntroduceGuardInspection : BaseInspection() {
    companion object {
        const val INSPECTION_NAME = "The alternate (else) branch is much lighter - consider inverting the if"
        const val FIX_NAME = "Invert 'if' to simplify method"
    }

    override fun buildErrorString(vararg infos: Any?): String {
        return INSPECTION_NAME
    }

    override fun buildVisitor() = IntroduceGuardVisitor()

    class IntroduceGuardVisitor : BaseInspectionVisitor() {
        override fun visitIfStatement(ifStatement: PsiIfStatement?) {
            if (ifStatement == null) return

            val applicable =
                complexity(ifStatement.thenBranch) > 3 &&
                        complexity(ifStatement.elseBranch) == 0

            if (applicable) {
                registerError(ifStatement.firstChild, "A")
            }
        }

        private fun complexity(block: PsiStatement?): Int =
            if (block == null) 0
            else CognitiveComplexityVisitor().visitElement(block).total()
    }

    override fun buildFix(vararg infos: Any?) = FlipIfFix()

    class FlipIfFix : InspectionGadgetsFix() {
        override fun getFamilyName(): String {
            return FIX_NAME
        }

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            if (project == null) return
            if (descriptor == null) return
            val document =
                PsiDocumentManager.getInstance(project).getDocument(descriptor.psiElement.containingFile) ?: return
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project, "Flip 'if'", "Live-Coding", {
                    val action = InvertIfConditionAction()
                    val editor = ImaginaryEditor(project, document)
                    if (action.isAvailable(project, editor, descriptor.psiElement)) {
                        action.invoke(project, editor, descriptor.psiElement)
                    }
                })
            }
        }
    }
}


