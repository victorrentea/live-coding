package com.github.victorrentea.livecoding.varie

import com.github.victorrentea.livecoding.complexity.CognitiveComplexityVisitor
import com.intellij.codeInsight.intention.impl.InvertIfConditionAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiStatement
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix


class IntroduceGuardInspection : BaseInspection() {

    override fun buildVisitor() = IntroduceGuardVisitor()

    override fun buildErrorString(vararg infos: Any?): String {
        return "The alternate (else) branch is much lighter - consider inverting the if"
    }

//    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
//        return MyFix()
//    }

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

//    class MyFix : InspectionGadgetsFix() {
//        override fun getFamilyName(): String {
//            return "Invert 'if' to simplify method"
//        }
//
//        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
//            ApplicationManager.getApplication().invokeLater {
//                val action = InvertIfConditionAction()
//                val editor = FileEditorManager.getInstance(descriptor!!.psiElement.project).selectedTextEditor
//                if (!action.isAvailable(project!!, null, descriptor.psiElement)) {
//                    action.invoke(project, editor, descriptor.psiElement)
//                }
//            }
//        }
//    }
}


