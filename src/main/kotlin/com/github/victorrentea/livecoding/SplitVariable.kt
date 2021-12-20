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
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes


class SplitVariableInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return SplitVariableVisitor(holder)
    }
}

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
        if (true) return // WIP
        if (psiLocalVar !is PsiLocalVariable) return
        val declarationBlock = psiLocalVar.containingBlock ?: return

        println("\nEXAMINE SPLIT of $psiLocalVar at line ${psiLocalVar.getLineNumber()}")

        val referencesToMe = psiLocalVar.referencesToMe
        println("Found ${referencesToMe.size} references at lines " + referencesToMe.map { it.getLineNumber() })

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
        }
    }
}

val PsiElement.containingBlock get() = PsiTreeUtil.getParentOfType(this, PsiCodeBlock::class.java)
val PsiVariable.referencesToMe get() =
        PsiTreeUtil.findChildrenOfType(parentOfTypes(PsiMethod::class), PsiReferenceExpression::class.java)
            .filter { it.resolve() == this }


class SplitVariableQuickFix(psiLocalVariable: PsiLocalVariable) : LocalQuickFixOnPsiElement(psiLocalVariable) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = Constants.SPLIT_VARIABLE_DESCRIPTION

    override fun invoke(
        project: Project, file: PsiFile, psiLocalVariable: PsiElement, endElement: PsiElement
    ) {
        if (psiLocalVariable !is PsiLocalVariable) return

        val declarationBlock = PsiTreeUtil.getParentOfType(psiLocalVariable, PsiCodeBlock::class.java)
        val references = PsiTreeUtil.findChildrenOfType(declarationBlock, PsiReferenceExpression::class.java)
            .filter { it.resolve() == psiLocalVariable}

        val assignments = references
            .filter { (it.parent as? PsiAssignmentExpression)?.lExpression == it }
            .map { it.parent as PsiAssignmentExpression }

        val firstAssignmentsInBlock = assignments.groupBy { PsiTreeUtil.getParentOfType(it, PsiCodeBlock::class.java) }
            .mapValues { it.value[0] }
            .values

        WriteCommandAction.runWriteCommandAction(project, Constants.SPLIT_VARIABLE_DESCRIPTION, "Live-Coding", {

            for (firstAssignmentInBlock in firstAssignmentsInBlock) {
                replaceAssignmentWithDeclaration(firstAssignmentInBlock, psiLocalVariable, psiLocalVariable.name)
            }
            (psiLocalVariable.parent as? PsiDeclarationStatement)?.delete()

        })
    }


}
