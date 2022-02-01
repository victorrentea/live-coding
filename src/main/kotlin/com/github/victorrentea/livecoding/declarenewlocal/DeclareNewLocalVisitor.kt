package com.github.victorrentea.livecoding.declarenewlocal

import com.github.victorrentea.livecoding.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.isAncestor
import com.intellij.psi.util.parentsOfType
import com.siyeh.ig.BaseInspectionVisitor

class DeclareNewLocalVisitor : BaseInspectionVisitor() {
    companion object{
        private val log = logger<DeclareNewLocalVisitor>()
    }
    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        if (element !is PsiLocalVariable && element !is PsiParameter) return
        val psiLocalVar: PsiVariable = element as PsiVariable

        val referencesToMe = psiLocalVar.referencesToMe
        log.debug("Try to define a NEW LOCAL ${psiLocalVar.name} referenced on lines " +
                referencesToMe.map { ":" + it.getLineNumber() + "(" + (if (it.isRead()) "R" else "W") + ")" })


        var i = 0
        while (i < referencesToMe.size && referencesToMe[i].containingBlock == psiLocalVar.containingBlock && referencesToMe[i].isWrite()) i++ // skip redundant initializers

        while (i < referencesToMe.size) {
            while (i < referencesToMe.size && referencesToMe[i].isRead()) i++ // skip
            if (i == referencesToMe.size) break;
            // i = first write after reads
            while (i + 1 < referencesToMe.size && referencesToMe[i + 1].isWrite()) i++ // skip to the last consecutive write
            // i = last write after reads
            if (i + 1 < referencesToMe.size) { // there are more references after (Reads)
                val writeToDeclareAt = referencesToMe[i]
                // there are reads after me
                log.debug("Trying to split at assignment on line " + writeToDeclareAt.getLineNumber())

                val laterUsages = referencesToMe.drop(i + 1)

                if (laterUsages.isNotEmpty()
                    && neverReadLaterInParentBlock(writeToDeclareAt, laterUsages)
                    && !inALoop(writeToDeclareAt)
                    && !inACase(writeToDeclareAt)
                ) {
                    // values never "leak out of this block"

                   if (!DeclareNewLocalFix.supportsDeclarationForWrite(writeToDeclareAt)) return

                    log.debug("ADDED PROBLEM")
                    registerError(writeToDeclareAt)
                } else {
                    log.debug("Some later usages are not in child blocks")
                }
                i++
            }

        }
    }

    private fun inACase(writeToDeclareAt: PsiReferenceExpression): Boolean =
        writeToDeclareAt.containingBlock?.parent is PsiSwitchStatement

    private fun inALoop(writeToDeclareAt: PsiReferenceExpression) = writeToDeclareAt.parentsOfType(PsiLoopStatement::class.java).count() > 0

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
                log.debug("FOUND WRITE in parent")
                return true
            }
            if (laterUsage.isRead()) {
                log.debug("FOUND READ in parent")
                return false
            }
        }
        log.debug("FINISHED never read")
        return true
    }

    private fun blockTerminatesFunction(block: PsiCodeBlock): Boolean =
        PsiTreeUtil.getChildOfAnyType(block, PsiReturnStatement::class.java) != null ||
                PsiTreeUtil.getChildOfAnyType(block, PsiThrowStatement::class.java) != null


}