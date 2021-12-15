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

class SplitVariableVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(psiLocalVar: PsiElement) {
        super.visitElement(psiLocalVar)

        if (psiLocalVar !is PsiLocalVariable) return
        val declarationBlock = PsiTreeUtil.getParentOfType(psiLocalVar, PsiCodeBlock::class.java) ?: return

        println("\n\n\n\nSTART with $declarationBlock")

        val allReferences = PsiTreeUtil.findChildrenOfType(declarationBlock, PsiReferenceExpression::class.java)
        if (allReferences.any { it.resolve() == null }) return

        val referencesToMe = allReferences
            .filter { it.resolve() == psiLocalVar }
        println("Found ${referencesToMe.size} references")

        val assignments = referencesToMe.filter { (it.parent as? PsiAssignmentExpression)?.lExpression == it }
        println("Assignments: " + assignments)

        val blocks = referencesToMe.map { it.parentCodeBlock()!! }
            .map { PsiTreeUtil.getTopmostParentOfType(it, PsiForStatement::class.java)?.parentCodeBlock() ?: it}
            .map { if (it.isAncestor(declarationBlock)) declarationBlock else it}
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

        if (topBlocks.size == 1) return

        if (false) // WIP
        holder.registerProblem(
            psiLocalVar,
            "Variable can be split in separate declarations",
            ProblemHighlightType.WARNING,
            SplitVariableQuickFix(psiLocalVar)
        )


    }

}


private fun PsiElement.parentCodeBlock() =
    PsiTreeUtil.getParentOfType(this, PsiCodeBlock::class.java)

//

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

        val blocks = references.map { PsiTreeUtil.getParentOfType(it, PsiCodeBlock::class.java) }.distinct()


        val firstAssignmentsInBlock = assignments.groupBy { PsiTreeUtil.getParentOfType(it, PsiCodeBlock::class.java) }
            .mapValues { it.value[0] }
            .values

        WriteCommandAction.runWriteCommandAction(project, "Split Variable", "Live-Coding", {

            for (firstAssignmentInBlock in firstAssignmentsInBlock) {
                replaceAssignmentWithDeclaration(firstAssignmentInBlock, psiLocalVariable)
            }
            (psiLocalVariable.parent as? PsiDeclarationStatement)?.delete()

        })


    }

    private fun replaceAssignmentWithDeclaration(
        firstAssignmentInBlock: PsiAssignmentExpression,
        psiLocalVariable: PsiLocalVariable
    ) {
        val psiFactory = JavaPsiFacade.getElementFactory(firstAssignmentInBlock.project)
        val newDeclaration = psiFactory.createVariableDeclarationStatement(
                psiLocalVariable.name, psiLocalVariable.type, firstAssignmentInBlock.rExpression
            )
        firstAssignmentInBlock.parent.replace(newDeclaration)
    }

}