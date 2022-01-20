package com.github.victorrentea.livecoding.extracthints

import com.github.victorrentea.livecoding.startLineNumber
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import org.mozilla.javascript.ast.IfStatement

class SyntaxExtractableSectionsVisitor {
    companion object {
        private val log = logger<SyntaxExtractableSectionsVisitor>()
    }
    private val sections = mutableSetOf<List<PsiStatement>>()

    fun getSections() = sections.toList().sortedWith(compareBy({it.first().startOffset}, {it.last().startOffset}))

    fun visitElement(element: PsiElement) {
        if (element is PsiCodeBlock) {
            for (i in 0 until element.statements.size) {
                for (j in i until element.statements.size) {
                    val tentativeStatements = (i..j).toList().map { element.statements[it] !! }.toList()

                    if (tentativeStatements.any { it is PsiSwitchLabelStatement }) continue

                    if (tentativeStatements.any { hasOrphanBreakContinueChildren(it)}) continue

                    if (partialReturns(tentativeStatements)) continue
                    

                    sections += tentativeStatements
                }
            }
        }

        element.children.forEach { visitElement(it) }
    }

    private fun partialReturns(tentativeStatements: List<PsiStatement>): Boolean {
        // returns before the last statement
        if (tentativeStatements.subList(0, tentativeStatements.size - 1).any { containsReturns(it) })
            return true

        val lastStatement = tentativeStatements.last()
        if (!containsReturns(lastStatement)) return false

        return !returnsOnAllBranches(lastStatement)
    }
    private fun returnsOnAllBranches(statement: PsiStatement):Boolean {
        if (statement is PsiReturnStatement) return true
        if (statement is PsiBlockStatement) return !partialReturns(statement.codeBlock.statements.toList())
        if (statement is PsiIfStatement) {
            if (statement.thenBranch?.let{returnsOnAllBranches(it)} == false) return false
            if (statement.elseBranch?.let{returnsOnAllBranches(it)} == false) return false
            return true
        }
        if (statement is PsiLoopStatement) {
            return statement.body?.let {containsReturns(it) } == false
        }
        return true;
    }

    private fun containsReturns(it: PsiStatement) =
        PsiTreeUtil.findChildrenOfType(it, PsiReturnStatement::class.java).isNotEmpty()

    private fun incompleteReturn(it: PsiStatement) =
        PsiTreeUtil.findChildrenOfType(it, PsiReturnStatement::class.java).isNotEmpty()

    private fun hasOrphanBreakContinueChildren(statement: PsiStatement): Boolean {
        // a break or continue must all have a parent for/while/dowhile that is a child of statement
        val blockInterruptions = PsiTreeUtil.findChildrenOfAnyType(
            statement,
            PsiContinueStatement::class.java,
            PsiBreakStatement::class.java
        )
        for(interruption in blockInterruptions) {
            val loopParents = PsiTreeUtil.collectParents(interruption, PsiLoopStatement::class.java, false) {it != statement}.size
            if (loopParents == 0) {
                log.trace("Found an orphan break/continue at line " + interruption.startLineNumber())
                return true
            }
        }
        return false
    }
}