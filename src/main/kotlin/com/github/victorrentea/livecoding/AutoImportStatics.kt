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
    val qualifiedMethodNames = listOf(
        "org.assertj.core.api.Assertions#assertThat",
        "java.util.stream.Collectors#toSet",
        "java.util.stream.Collectors#toList",
        "org.mockito.Mockito#mock",
        "org.mockito.Mockito#when",
        "org.mockito.Mockito#verify",
    )
}
class AutoImportStaticsVisitor : PsiRecursiveElementWalkingVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element !is PsiMethodCallExpression) return
        if (element.methodExpression.qualifierExpression == null) return
        val calledPsiMethod = element.methodExpression.resolve() ?: return
        if (calledPsiMethod !is PsiMethod) return
        if (!calledPsiMethod.hasModifierProperty(PsiModifier.STATIC)) return

        val containingClass = calledPsiMethod.containingClass ?: return
        val classFQName = containingClass.qualifiedName ?: return
        val qualifiedMethodName = classFQName + "#" + calledPsiMethod.name

        //println("Looking at " + qualifiedMethodName)
        if (!qualifiedMethodNames.contains(qualifiedMethodName)) return

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(element.project, "Auto-Import Statics", "Live-Coding", {
                ImportUtils.addStaticImport(classFQName, calledPsiMethod.name, element)
                val qualifierExpression = element.methodExpression.qualifierExpression
                qualifierExpression?.delete()
            })
        }
    }
}
