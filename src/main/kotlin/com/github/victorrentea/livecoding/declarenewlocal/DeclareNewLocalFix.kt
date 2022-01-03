package com.github.victorrentea.livecoding.declarenewlocal

import com.github.victorrentea.livecoding.containingBlock
import com.github.victorrentea.livecoding.referencesToMe
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.ide.DataManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.isAncestor
import com.intellij.psi.util.siblings
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.refactoring.suggested.endOffset
import org.jetbrains.concurrency.asCompletableFuture

class DeclareNewLocalFix(localVariable: PsiVariable, reassignment: PsiAssignmentExpression) :
    LocalQuickFixOnPsiElement(localVariable, reassignment) {

    companion object {
        const val FIX_NAME = "Declare new variable here"
        private val log = logger<DeclareNewLocalFix>()
        val supportedAssignmentTokens = listOf(
            JavaTokenType.PLUSEQ,
            JavaTokenType.MINUSEQ,
            JavaTokenType.ASTERISKEQ,
            JavaTokenType.DIVEQ,
            JavaTokenType.PERCEQ,
            JavaTokenType.EQ
        )
    }

    override fun getFamilyName() = "Live-Coding"

    override fun getText() = FIX_NAME

    override fun invoke(project: Project, file: PsiFile, localVariable: PsiElement, reassignment: PsiElement) {
        if (localVariable !is PsiVariable) return
        if (reassignment !is PsiAssignmentExpression) return

        log.debug(" ---------- act ${localVariable.name} ---------")
        val usages = localVariable.referencesToMe
        val usagesOfNewVariable = usages.drop(usages.indexOf(reassignment.lExpression) + 1)
            .filter { reassignment.containingBlock.isAncestor(it) }

        WriteCommandAction.runWriteCommandAction(project, FIX_NAME, "Live-Coding", {
            val textOffset = reassignment.lExpression.endOffset
            val editor = FileEditorManager.getInstance(project).selectedEditor as TextEditor
            editor.editor.caretModel.moveToOffset(textOffset)

            replaceAssignmentWithDeclaration(reassignment, localVariable, localVariable.name + "_")
            for (psiReferenceExpression in usagesOfNewVariable) {
                val elementFactory = JavaPsiFacade.getElementFactory(project)
                val ref = elementFactory.createExpressionFromText(localVariable.name + "_", psiReferenceExpression);
                psiReferenceExpression.replace(ref)
            }

            DataManager.getInstance().dataContextFromFocusAsync.asCompletableFuture()
                .thenAccept { dataContext ->
                    val renameHandler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext)
                    renameHandler?.invoke(project, editor.editor, file, dataContext)
                }
        })
    }


    fun replaceAssignmentWithDeclaration(
        assignment: PsiAssignmentExpression,
        psiLocalVariable: PsiVariable,
        variableName: String
    ) {
        val psiFactory = JavaPsiFacade.getElementFactory(assignment.project)
        val declarationInit = when (assignment.operationSign.tokenType) {
            JavaTokenType.PLUSEQ -> createBinaryExpression(psiLocalVariable.name!!, "+", assignment.rExpression!!)
            JavaTokenType.MINUSEQ -> createBinaryExpression(psiLocalVariable.name!!, "-", assignment.rExpression!!)
            JavaTokenType.ASTERISKEQ -> createBinaryExpression(psiLocalVariable.name!!, "*", assignment.rExpression!!)
            JavaTokenType.DIVEQ -> createBinaryExpression(psiLocalVariable.name!!, "/", assignment.rExpression!!)
            JavaTokenType.PERCEQ -> createBinaryExpression(psiLocalVariable.name!!, "%", assignment.rExpression!!)
            JavaTokenType.EQ -> assignment.rExpression
            else -> throw IllegalArgumentException("Unsupported token in assignment: " + assignment.text)
        }
        val newDeclaration = psiFactory.createVariableDeclarationStatement(
            variableName, psiLocalVariable.type, declarationInit
        )

        assignment.siblings(withSelf = false)
            .dropWhile { (it as? PsiJavaToken)?.tokenType == JavaTokenType.SEMICOLON }
            .forEach { newDeclaration.add(it) }
        assignment.parent.replace(newDeclaration)
    }


    private fun createBinaryExpression(
        firstOperandString: String,
        operatorString: String,
        secondOperand: PsiExpression
    ): PsiBinaryExpression {
        val elementFactory = JavaPsiFacade.getElementFactory(secondOperand.project)
        val div = elementFactory.createExpressionFromText(
            "$firstOperandString $operatorString 1",
            secondOperand
        ) as PsiBinaryExpression
        div.rOperand!!.replace(secondOperand)
        return div
    }

}