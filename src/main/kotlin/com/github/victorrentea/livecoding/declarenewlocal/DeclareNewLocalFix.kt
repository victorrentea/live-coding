package com.github.victorrentea.livecoding.declarenewlocal

import com.github.victorrentea.livecoding.containingBlock
import com.github.victorrentea.livecoding.referencesToMe
import com.intellij.codeInspection.ProblemDescriptor
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
import com.intellij.refactoring.suggested.startOffset
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.concurrency.asCompletableFuture

class DeclareNewLocalFix : InspectionGadgetsFix() {
    companion object {
        private val log = logger<DeclareNewLocalFix>()

        val supportedAssignmentTokens = listOf(
            JavaTokenType.PLUSEQ,
            JavaTokenType.MINUSEQ,
            JavaTokenType.ASTERISKEQ,
            JavaTokenType.DIVEQ,
            JavaTokenType.PERCEQ,
            JavaTokenType.EQ
        )
        fun supportsDeclarationForWrite(writeToDeclareAt: PsiReferenceExpression): Boolean {
            val assignToSplit = writeToDeclareAt.parent as? PsiAssignmentExpression ?: return false
            if (assignToSplit.rExpression == null) return false
            if (!supportedAssignmentTokens.contains(assignToSplit.operationSign.tokenType))  return false
            return true
        }

        fun replaceAssignmentWithDeclaration(
            writeToDeclareAt: PsiReferenceExpression,
            variableName: String
        ): PsiDeclarationStatement? {
            val assignment = writeToDeclareAt.parent as? PsiAssignmentExpression ?:return null
            val psiLocalVariable = writeToDeclareAt.resolve() as? PsiLocalVariable ?:return null
            val psiFactory = JavaPsiFacade.getElementFactory(assignment.project)
            val declarationInit = when (assignment.operationSign.tokenType) {
                JavaTokenType.PLUSEQ -> createBinaryExpression(psiLocalVariable.name, "+", assignment.rExpression!!)
                JavaTokenType.MINUSEQ -> createBinaryExpression(psiLocalVariable.name, "-", assignment.rExpression!!)
                JavaTokenType.ASTERISKEQ -> createBinaryExpression(psiLocalVariable.name, "*", assignment.rExpression!!)
                JavaTokenType.DIVEQ -> createBinaryExpression(psiLocalVariable.name, "/", assignment.rExpression!!)
                JavaTokenType.PERCEQ -> createBinaryExpression(psiLocalVariable.name, "%", assignment.rExpression!!)
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
            return newDeclaration
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

    override fun getFamilyName() = DeclareNewLocalInspection.FIX_NAME

    override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
        val writeToDeclareAt = descriptor?.psiElement as? PsiReferenceExpression ?: return

        val localVariable = writeToDeclareAt.resolve() as? PsiLocalVariable;
        if (localVariable == null) {
            println("NOT A LOCAL VAR ")
            return
        }
        if (project == null) return

        log.debug(" ---------- act ${localVariable.name} ---------")
        val usages = localVariable.referencesToMe
        val usagesOfNewVariable = usages.drop(usages.indexOf(writeToDeclareAt) + 1)
            .filter { writeToDeclareAt.containingBlock.isAncestor(it) }

        val file = descriptor.psiElement.containingFile

        WriteCommandAction.runWriteCommandAction(project, DeclareNewLocalInspection.FIX_NAME, "Live-Coding", {

            println("RUN WRITE")
            val writeStartOffset = writeToDeclareAt.startOffset
            println("original OFFSET : $writeStartOffset")
            val newDeclaration = replaceAssignmentWithDeclaration(writeToDeclareAt, localVariable.name + "_") ?: return@runWriteCommandAction
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            for (psiReferenceExpression in usagesOfNewVariable) {
                val ref = elementFactory.createExpressionFromText(localVariable.name + "_", psiReferenceExpression)
                psiReferenceExpression.replace(ref)
            }
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@runWriteCommandAction
            documentManager.doPostponedOperationsAndUnblockDocument(document)
            val decl = newDeclaration.declaredElements[0] as PsiLocalVariable
            val declOffset = decl.nameIdentifier?.startOffsetInParent ?: 99
            val finalOffset = writeStartOffset + declOffset
            println("decl OFFSET : $finalOffset")


            DataManager.getInstance().dataContextFromFocusAsync.asCompletableFuture()
                .thenAccept { dataContext ->
                    val editor = FileEditorManager.getInstance(project).selectedEditor as TextEditor
                    editor.editor.caretModel.moveToOffset(finalOffset)
                    // open the rename refactor
                    val renameHandler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext)
                    renameHandler?.invoke(project, editor.editor, file, dataContext)
                }
        })
    }






}