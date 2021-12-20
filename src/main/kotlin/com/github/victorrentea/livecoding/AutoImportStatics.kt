package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ImportUtils

class AutoImportStatics : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.getData(CommonDataKeys.PSI_FILE)?.accept(AutoImportStaticsVisitor())
    }
}
class AutoImportStaticsVisitor : PsiRecursiveElementWalkingVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element is PsiReferenceExpression) {
            if (!AppSettingsState.getInstance().staticImports.containsKey(element.referenceName)) return
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
        val desiredClassQName = AppSettingsState.getInstance().staticImports[methodName] ?: return

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

        val desiredClassQName = AppSettingsState.getInstance().staticImports[staticReference.referenceName]

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
