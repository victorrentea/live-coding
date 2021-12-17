package com.github.victorrentea.livecoding

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.isAncestor


class SplitVariableInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return SplitVariableVisitor(holder)
    }
}

fun PsiReferenceExpression.isAssigned() = (parent as? PsiAssignmentExpression)?.lExpression == this

fun PsiElement.getLineNumber(): Int {
    return getLineNumber(this.containingFile, this.textOffset)
}

fun getLineNumber(psiFile: PsiFile, offset:Int): Int {
    val fileViewProvider = psiFile.viewProvider
    val document = fileViewProvider.document
    return document?.getLineNumber(offset)?.let { it + 1 } ?: -1
}

class SplitVariableVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(psiLocalVar: PsiElement) {
        super.visitElement(psiLocalVar)
//        if (true) return // WIP
        if (psiLocalVar !is PsiLocalVariable) return
        val declarationBlock = psiLocalVar.containingBlock ?: return

        println("\n\n\n\nSTART with declaration $psiLocalVar in ${declarationBlock.text} at line ${psiLocalVar.getLineNumber()}")

        val referencesToMe = psiLocalVar.referencesToMe
        println("Found ${referencesToMe.size} references at lines " + referencesToMe.map { it.getLineNumber() })


        val assignments = referencesToMe.filter { it.isAssigned() }
        println("Assignments: " + assignments)

        val blocks = referencesToMe.map { it.containingBlock!! }
            .map { PsiTreeUtil.getTopmostParentOfType(it, PsiForStatement::class.java)?.containingBlock ?: it }
            .map { if (it.isAncestor(declarationBlock)) declarationBlock else it }
            .distinct()
        println("blocks: $blocks")


        val topBlocks = mutableListOf<PsiCodeBlock>()
        for (block in blocks) {
            topBlocks.removeIf { block.isAncestor(it) }
            if (topBlocks.none { it.isAncestor(block) }) {
                topBlocks.add(block)
            }
        }

        println("Top blocks: ${topBlocks.size}")

        if (topBlocks.size > 1) {
            println("can break variable in exclusive blocks")
            holder.registerProblem(
                psiLocalVar,
                Constants.SPLIT_VARIABLE_DESCRIPTION,
                ProblemHighlightType.WARNING,
                SplitVariableQuickFix(psiLocalVar)
            )
        } else {
            fun PsiReferenceExpression.isWrite() = isAssigned()
            fun PsiReferenceExpression.isRead() = !isAssigned()

            var i = 1;
            while (i < referencesToMe.size) {
                while (i < referencesToMe.size && referencesToMe[i].isRead()) i++ // skip
                if (i == referencesToMe.size) break;
                // i = write
                while (i < referencesToMe.size && referencesToMe[i].isWrite()) i++ // skip to the last
                i-- // i = last write after reads
                if (i + 1 < referencesToMe.size) {
                    // there are reads after me
                    println("Trying to split at assignment on line " + referencesToMe[i].getLineNumber())

                    // all later usages are in this or children blocks
                    val laterUsages = referencesToMe.drop(i + 1)
                    if (laterUsages.all { referencesToMe[i].containingBlock.isAncestor(it) }) {
                        val assignToSplit = referencesToMe[i].parent as? PsiAssignmentExpression ?: return
                        println("ADDED PROBLEM")
                        holder.registerProblem(
                            assignToSplit,
                            Constants.SPLIT_VARIABLE_DESCRIPTION,
                            ProblemHighlightType.WARNING,
                            DefineNewVariable(psiLocalVar, assignToSplit)
                        )
                    } else {
                        println("Some later usages are not in child blocks")
                    }
                    i++
                }

            }
        }
    }
}

private val PsiElement.containingBlock get() = PsiTreeUtil.getParentOfType(this, PsiCodeBlock::class.java)
private val PsiLocalVariable.referencesToMe
    get() =
        PsiTreeUtil.findChildrenOfType(containingBlock, PsiReferenceExpression::class.java)
            .filter { it.resolve() == this }


class DefineNewVariable(localVariable: PsiLocalVariable, reassignment: PsiAssignmentExpression) :
    LocalQuickFixOnPsiElement(localVariable, reassignment) {

    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Split variable"

    override fun invoke(project: Project, file: PsiFile, localVariable: PsiElement, reassignment: PsiElement) {
        if (localVariable !is PsiLocalVariable) return
        if (reassignment !is PsiAssignmentExpression) return

        println(" ---------- act ${localVariable.name} ---------")
        val usages = localVariable.referencesToMe
        val usagesOfNewVariable = usages.drop(usages.indexOf(reassignment.lExpression) + 1)

        WriteCommandAction.runWriteCommandAction(project, "Split Variable", "Live-Coding", {
            replaceAssignmentWithDeclaration(reassignment, localVariable, localVariable.name + "_")
            for (psiReferenceExpression in usagesOfNewVariable) {
                val elementFactory = JavaPsiFacade.getElementFactory(project)
                val ref = elementFactory.createExpressionFromText(localVariable.name + "_", psiReferenceExpression);
//                println("Replacing $psiReferenceExpression with $ref at line ${psiReferenceExpression.getLineNumber()}")
                psiReferenceExpression.replace(ref)
            }
        })
    }
}

//
class SplitVariableQuickFix(psiLocalVariable: PsiLocalVariable) : LocalQuickFixOnPsiElement(psiLocalVariable) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Split variable"

    override fun invoke(
        project: Project, file: PsiFile, psiLocalVariable: PsiElement, endElement: PsiElement
    ) {
        if (psiLocalVariable !is PsiLocalVariable) return

        val declarationBlock = PsiTreeUtil.getParentOfType(psiLocalVariable, PsiCodeBlock::class.java)
        val references = PsiTreeUtil.findChildrenOfType(declarationBlock, PsiReferenceExpression::class.java)
            .filter { it.resolve() == psiLocalVariable }

        val assignments = references
            .filter { (it.parent as? PsiAssignmentExpression)?.lExpression == it }
            .map { it.parent as PsiAssignmentExpression }

        val firstAssignmentsInBlock = assignments.groupBy { PsiTreeUtil.getParentOfType(it, PsiCodeBlock::class.java) }
            .mapValues { it.value[0] }
            .values

        WriteCommandAction.runWriteCommandAction(project, "Split Variable", "Live-Coding", {

            for (firstAssignmentInBlock in firstAssignmentsInBlock) {
                replaceAssignmentWithDeclaration(firstAssignmentInBlock, psiLocalVariable, psiLocalVariable.name)
            }
            (psiLocalVariable.parent as? PsiDeclarationStatement)?.delete()

        })
    }


}

private fun replaceAssignmentWithDeclaration(
    psiAssignmentExpression: PsiAssignmentExpression,
    psiLocalVariable: PsiLocalVariable,
    variableName: String
) {
    val psiFactory = JavaPsiFacade.getElementFactory(psiAssignmentExpression.project)
    val newDeclaration = psiFactory.createVariableDeclarationStatement(
        variableName, psiLocalVariable.type, psiAssignmentExpression.rExpression
    )
    psiAssignmentExpression.parent.replace(newDeclaration)
}
