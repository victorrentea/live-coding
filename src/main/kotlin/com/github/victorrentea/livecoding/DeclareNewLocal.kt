package com.github.victorrentea.livecoding

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.DataManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.refactoring.suggested.endOffset
import com.intellij.util.PsiErrorElementUtil
import org.jetbrains.concurrency.asCompletableFuture


class DeclareNewLocalInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Local variable semantics might be confusing";
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DeclareNewLocalVisitor(holder)
    }
}

class DeclareNewLocalVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(psiLocalVar: PsiElement) {
        if (PsiErrorElementUtil.hasErrors(psiLocalVar.project, psiLocalVar.containingFile.virtualFile)) return;

        super.visitElement(psiLocalVar)
        if (psiLocalVar !is PsiLocalVariable) return

        val referencesToMe = psiLocalVar.referencesToMe
        println("\nEXAMINE DEFINE NEW LOCAL ${psiLocalVar.name} referenced on lines " + referencesToMe.map { ":" + it.getLineNumber() + "(" + (if (it.isRead()) "R" else "W") + ")" })


        var i = 0
        while (i < referencesToMe.size) {
            while (i < referencesToMe.size && referencesToMe[i].isRead()) i++ // skip
            if (i == referencesToMe.size) break;
            // i = first write after reads
            while (i + 1 < referencesToMe.size && referencesToMe[i + 1].isWrite()) i++ // skip to the last consecutive write
            // i = last write after reads
            if (i + 1 < referencesToMe.size) { // there are more references after (Reads)
                val writeToDeclareAt = referencesToMe[i]
                // there are reads after me
                println("Trying to split at assignment on line " + writeToDeclareAt.getLineNumber())

                val laterUsages = referencesToMe.drop(i + 1)

                if (laterUsages.isNotEmpty()
                    && neverReadLaterInParentBlock(writeToDeclareAt, laterUsages)
                    && !inALoop(writeToDeclareAt)
                    && !inACase(writeToDeclareAt)
                ) {
                    // values never "leak out of this block"
                    val assignToSplit = writeToDeclareAt.parent as? PsiAssignmentExpression ?: return
                    if (assignToSplit.rExpression == null) return

                    println("ADDED PROBLEM")
                    holder.registerProblem(
                        assignToSplit,
                        DeclareNewLocalInspection.INSPECTION_NAME,
                        ProblemHighlightType.WARNING,
                        DeclareNewLocalFix(psiLocalVar, assignToSplit)
                    )
                } else {
                    println("Some later usages are not in child blocks")
                }
                i++
            }

        }
    }

    private fun inACase(writeToDeclareAt: PsiReferenceExpression): Boolean =
        writeToDeclareAt.containingBlock?.parent is PsiSwitchStatement

    private fun inALoop(writeToDeclareAt: PsiReferenceExpression) = writeToDeclareAt.parents.any {
        it is PsiForStatement ||
                it is PsiForeachStatement ||
                it is PsiWhileStatement ||
                it is PsiDoWhileStatement
    }


    private fun neverReadLaterInParentBlock(
        writeToDeclareAt: PsiReferenceExpression,
        laterUsages: List<PsiReferenceExpression>
    ): Boolean {

        val parentIfs = writeToDeclareAt.parentsOfType(PsiIfStatement::class.java)

        val parentBlockTerminatingMethod = writeToDeclareAt.parentsOfType(PsiCodeBlock::class.java)
            .firstOrNull { blockTerminatesFunction(it) }

        for (laterUsage in laterUsages) {
            val usageUnderDeclarationBlock = writeToDeclareAt.containingBlock.isAncestor(laterUsage)
            if (usageUnderDeclarationBlock) continue

            if (parentIfs.any { it.elseBranch.isAncestor(laterUsage) }) continue // usage on else branch

            if (parentBlockTerminatingMethod != null && !parentBlockTerminatingMethod.isAncestor(laterUsage)) {
                return true
            }

            if (laterUsage.isWrite()) {
                println("FOUND WRITE in parent")
                return true
            }
            if (laterUsage.isRead()) {
                println("FOUND READ in parent")
                return false
            }
        }
        println("FINISHED never read")
        return true
    }

    fun blockTerminatesFunction(block: PsiCodeBlock): Boolean =
        PsiTreeUtil.getChildOfAnyType(block, PsiReturnStatement::class.java) != null ||
                PsiTreeUtil.getChildOfAnyType(block, PsiThrowStatement::class.java) != null


    fun PsiReferenceExpression.isWrite() = isAssigned()
    fun PsiReferenceExpression.isRead() = !isAssigned()
}

fun PsiReferenceExpression.isAssigned() = (parent as? PsiAssignmentExpression)?.lExpression == this


class DeclareNewLocalFix(localVariable: PsiLocalVariable, reassignment: PsiAssignmentExpression) :
    LocalQuickFixOnPsiElement(localVariable, reassignment) {

    companion object {
        const val FIX_NAME = "Declare new variable here"
    }

    override fun getFamilyName() = "Live-Coding"

    override fun getText() = FIX_NAME

    override fun invoke(project: Project, file: PsiFile, localVariable: PsiElement, reassignment: PsiElement) {
        if (localVariable !is PsiLocalVariable) return
        if (reassignment !is PsiAssignmentExpression) return

        println(" ---------- act ${localVariable.name} ---------")
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
}

fun replaceAssignmentWithDeclaration(
    assignment: PsiAssignmentExpression,
    psiLocalVariable: PsiLocalVariable,
    variableName: String
) {
    val psiFactory = JavaPsiFacade.getElementFactory(assignment.project)
    val declarationInit = when (assignment.operationSign.tokenType) {
        JavaTokenType.PLUSEQ -> psiBinaryExpression(psiLocalVariable.name, "+", assignment.rExpression!!)
        JavaTokenType.MINUSEQ -> psiBinaryExpression(psiLocalVariable.name, "-", assignment.rExpression!!)
        JavaTokenType.ASTERISKEQ -> psiBinaryExpression(psiLocalVariable.name, "*", assignment.rExpression!!)
        JavaTokenType.DIVEQ -> psiBinaryExpression(psiLocalVariable.name, "/", assignment.rExpression!!)
        JavaTokenType.PERCEQ -> psiBinaryExpression(psiLocalVariable.name, "%", assignment.rExpression!!)
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

private fun psiBinaryExpression(
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
