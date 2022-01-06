package com.github.victorrentea.livecoding.extracthints

import com.github.victorrentea.livecoding.startLineNumber
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset

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
                    val tentativeSection = (i..j).toList().map { element.statements[it] !! }.toList()

                    if (tentativeSection.any { it is PsiSwitchLabelStatement }) continue

                    if (tentativeSection.any { hasOrphanBreakContinueChildren(it)}) continue

                    if (tentativeSection.any { PsiTreeUtil.countChildrenOfType(it, PsiReturnStatement::class.java) > 0 }) continue // TODO remove:  In fact, we could extract any block if all the exec paths end with a RETURN or THROW

                    sections += tentativeSection
                }
            }
        }

        element.children.forEach { visitElement(it) }
    }

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