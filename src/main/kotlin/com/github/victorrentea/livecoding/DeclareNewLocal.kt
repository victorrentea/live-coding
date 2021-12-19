package com.github.victorrentea.livecoding

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.isAncestor
import com.intellij.psi.util.siblings


class DeclareNewLocalInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DeclareNewLocalVisitor(holder)
    }
}

class DeclareNewLocalVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(psiLocalVar: PsiElement) {
        super.visitElement(psiLocalVar)
        if (psiLocalVar !is PsiLocalVariable) return

        val referencesToMe = psiLocalVar.referencesToMe
        println("\nEXAMINE DEFINE NEW LOCAL ${psiLocalVar.name} referenced on lines " + referencesToMe.map { it.getLineNumber() })

        val assignments = referencesToMe.filter { it.isAssigned() }
        println("Assignments: $assignments")

        var i = 1;
        while (i < referencesToMe.size) {
            while (i < referencesToMe.size && referencesToMe[i].isRead()) i++ // skip
            if (i == referencesToMe.size) break;
            // i = write
            while (i < referencesToMe.size && referencesToMe[i].isWrite()) i++ // skip to the last
            i--
            // i = last write after reads
            if (i + 1 < referencesToMe.size) {
                val writeToDeclareAt = referencesToMe[i]
                // there are reads after me
                println("Trying to split at assignment on line " + writeToDeclareAt.getLineNumber())

                val laterUsages = referencesToMe.drop(i + 1)


                if (laterUsages.isNotEmpty() && neverReadLaterInParentBlock(writeToDeclareAt, laterUsages)) {
                    // values never "leak out of this block"
                    val assignToSplit = writeToDeclareAt.parent as? PsiAssignmentExpression ?: return
                    println("ADDED PROBLEM")
                    holder.registerProblem(
                        assignToSplit,
                        Constants.DECLARE_NEW_LOCAL_INSPECTION_NAME,
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

    private fun neverReadLaterInParentBlock(
        writeToDeclareAt: PsiReferenceExpression,
        laterUsages: List<PsiReferenceExpression>
    ): Boolean {
        for (laterUsage in laterUsages) {
            if (!writeToDeclareAt.containingBlock.isAncestor(laterUsage)) {
                if (laterUsage.isWrite()) {
                    println("FOUND WRITE in parent")
                    return true
                }
                if (laterUsage.isRead()) {
                    println("FOUND READ in parent")
                    return false
                }
            }
        }
        println("FINISHED never read")
        return true
    }

    fun PsiReferenceExpression.isWrite() = isAssigned()
    fun PsiReferenceExpression.isRead() = !isAssigned()
}

fun PsiReferenceExpression.isAssigned() = (parent as? PsiAssignmentExpression)?.lExpression == this


class DeclareNewLocalFix(localVariable: PsiLocalVariable, reassignment: PsiAssignmentExpression) :
    LocalQuickFixOnPsiElement(localVariable, reassignment) {

    override fun getFamilyName() = "Live-Coding"

    override fun getText() = Constants.DECLARE_NEW_LOCAL_FIX_NAME

    override fun invoke(project: Project, file: PsiFile, localVariable: PsiElement, reassignment: PsiElement) {
        if (localVariable !is PsiLocalVariable) return
        if (reassignment !is PsiAssignmentExpression) return

        println(" ---------- act ${localVariable.name} ---------")
        val usages = localVariable.referencesToMe
        val usagesOfNewVariable = usages.drop(usages.indexOf(reassignment.lExpression) + 1)
            .filter { reassignment.containingBlock.isAncestor(it) }

        WriteCommandAction.runWriteCommandAction(project, Constants.DECLARE_NEW_LOCAL_FIX_NAME, "Live-Coding", {
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


fun replaceAssignmentWithDeclaration(
    psiAssignmentExpression: PsiAssignmentExpression,
    psiLocalVariable: PsiLocalVariable,
    variableName: String
) {
    val psiFactory = JavaPsiFacade.getElementFactory(psiAssignmentExpression.project)
    val newDeclaration = psiFactory.createVariableDeclarationStatement(
        variableName, psiLocalVariable.type, psiAssignmentExpression.rExpression
    )
    psiAssignmentExpression.siblings(withSelf = false)
        .dropWhile { (it as? PsiJavaToken)?.tokenType == JavaTokenType.SEMICOLON }
        .forEach { newDeclaration.add(it) }
    psiAssignmentExpression.parent.replace(newDeclaration)
}
