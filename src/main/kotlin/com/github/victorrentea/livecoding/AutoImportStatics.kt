package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.AutoImportConstants.qualifiedMethodNames
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ImportUtils

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
        "org.mockito.Mockito#mock",
        "org.mockito.Mockito#when",
        "org.mockito.Mockito#verify",
    )
    private fun parse(vararg qualifiedMethods :String) =
        qualifiedMethods.toList()
            .associate { it.substringAfter("#") to it.substringBefore("#") }
}
class AutoImportStaticsVisitor : PsiRecursiveElementWalkingVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element !is PsiMethodCallExpression) return

        if (!qualifiedMethodNames.containsKey(element.methodExpression.referenceName)) return

        if (element.methodExpression.qualifierExpression != null) {
            replaceWithStaticImport(element)
        } else {
            addStaticImport(element)
        }
    }

    private fun addStaticImport(methodCall: PsiMethodCallExpression) {
        if (methodCall.methodExpression.resolve() != null) return
        // unqualified method not resolved
        val methodName = methodCall.methodExpression.referenceName ?: return
        val desiredClassQName = qualifiedMethodNames[methodName] ?: return

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(methodCall.project, "Auto-Import Statics", "Live-Coding", {
                ImportUtils.addStaticImport(desiredClassQName, methodName, methodCall)
            })
        }
    }

    private fun replaceWithStaticImport(methodCall: PsiMethodCallExpression) {
        val calledPsiMethod = methodCall.methodExpression.resolve() ?: return
        if (calledPsiMethod !is PsiMethod) return
        if (!calledPsiMethod.hasModifierProperty(PsiModifier.STATIC)) return

        val containingClass = calledPsiMethod.containingClass ?: return
        val actualClassQName = containingClass.qualifiedName ?: return
//        val qualifiedMethodName = actualClassQName + "#" + calledPsiMethod.name

        val desiredClassQName = qualifiedMethodNames[methodCall.methodExpression.referenceName]

        //println("Looking at " + qualifiedMethodName)
        if (desiredClassQName != actualClassQName) return

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(methodCall.project, "Auto-Import Statics", "Live-Coding", {
                ImportUtils.addStaticImport(actualClassQName, calledPsiMethod.name, methodCall)
                val qualifierExpression = methodCall.methodExpression.qualifierExpression
                qualifierExpression?.delete()
            })
        }
    }
}
