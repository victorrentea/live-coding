package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.AutoImportConstants.qualifiedMethodNames
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.util.ui.TextTransferable
import com.siyeh.ig.psiutils.ImportUtils
import git4idea.branch.GitBranchUtil
import org.jetbrains.annotations.NotNull

class AutoImportStatics : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.getData(CommonDataKeys.PSI_FILE)?.accept(AutoImportStaticsVisitor())
    }
}
object AutoImportConstants {
    val qualifiedMethodNames = parse(
        "org.assertj.core.api.Assertions#assertThat",
        "java.util.stream.Collectors#toSet",
        "java.util.stream.Collectors#toList",
        "java.util.stream.Collectors#toMap",
        "java.util.stream.Collectors#groupingBy",
        "java.util.function.Predicate#not",
        "org.mockito.Mockito#mock",
        "org.mockito.Mockito#when",
        "org.mockito.Mockito#verify",
        "java.lang.System#currentTimeMillis",
        "org.mockito.ArgumentMatchers#anyInt",
        "org.mockito.ArgumentMatchers#any",
        "org.mockito.ArgumentMatchers#anyString",
        "org.mockito.ArgumentMatchers#anyLong",
        "java.util.concurrent.TimeUnit#MILLISECONDS",
        "java.time.Duration#ofSeconds",
        "java.time.Duration#ofMillis",
        "java.util.concurrent.CompletableFuture#completedFuture"
    )
    private fun parse(vararg qualifiedMethods :String) =
        qualifiedMethods.toList()
            .associate { it.substringAfter("#") to it.substringBefore("#") }
}
class AutoImportStaticsVisitor : PsiRecursiveElementWalkingVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element is PsiReferenceExpression) {
            if (!qualifiedMethodNames.containsKey(element.referenceName)) return
            if (element.qualifierExpression != null) {
                replaceWithStaticImport(element)
            } else {
                addStaticImport(element)
            }
        }
    }

    private fun addStaticImport(unresolvedReference: PsiReferenceExpression) {
        if (unresolvedReference.resolve() != null) return
        // unqualified method not resolved
        val methodName = unresolvedReference.referenceName ?: return
        val desiredClassQName = qualifiedMethodNames[methodName] ?: return

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(unresolvedReference.project, "Auto-Import Statics", "Live-Coding", {
                ImportUtils.addStaticImport(desiredClassQName, methodName, unresolvedReference)
            })
        }
    }

    private fun replaceWithStaticImport(staticReference: PsiReferenceExpression) {
        val calledPsiMethod = staticReference.resolve() ?: return
        if (calledPsiMethod !is PsiMember) return
        val name = calledPsiMethod.name ?: return
        if (!calledPsiMethod.hasModifierProperty(PsiModifier.STATIC)) return

        val containingClass = calledPsiMethod.containingClass ?: return
        val actualClassQName = containingClass.qualifiedName ?: return

        val desiredClassQName = qualifiedMethodNames[staticReference.referenceName]

        if (desiredClassQName != actualClassQName) return

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(staticReference.project, "Auto-Import Statics", "Live-Coding", {
                val importAdded = ImportUtils.addStaticImport(actualClassQName, name, staticReference)
                if (importAdded) {
                    val qualifierExpression = staticReference.qualifierExpression
                    qualifierExpression?.delete()
                }
            })
        }
    }
}
