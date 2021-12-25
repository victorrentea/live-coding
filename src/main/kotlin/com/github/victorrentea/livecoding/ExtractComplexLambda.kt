package com.github.victorrentea.livecoding

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.fixes.ExtractMethodFix


class ExtractComplexLambdaInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Extract complex lambda to a method"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return ExtractComplexLambdaVisitor(holder)
    }
}

class ExtractComplexLambdaVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        val lambda = element as? PsiLambdaExpression ?: return
        val lambdaBody = lambda.body
        if (lambdaBody !is PsiCodeBlock) return
        if (PsiTreeUtil.countChildrenOfType(lambdaBody, PsiExpressionStatement::class.java) == 1) return

        holder.registerProblem(
            lambdaBody,
            ExtractComplexLambdaInspection.INSPECTION_NAME,
            ProblemHighlightType.WARNING,
            TextRange(0, 1),
            ExtractMethodFix(),
        )
    }
}
