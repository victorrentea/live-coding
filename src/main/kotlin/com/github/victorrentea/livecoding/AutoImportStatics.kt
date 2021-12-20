package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ImportUtils
import git4idea.util.GitUIUtil
import java.awt.Color

class AutoImportStatics : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val visitor = AutoImportStaticsVisitor()
        e.getData(CommonDataKeys.PSI_FILE)?.accept(visitor)

        ApplicationManager.getApplication().invokeLater {
            visitor.report()?.let { reportString ->
                e.getData(CommonDataKeys.EDITOR)?.let { editor ->
                    HintManager.getInstance().showInformationHint(editor, reportString)
                }
            }
        }
    }
}
class AutoImportStaticsVisitor : PsiRecursiveElementWalkingVisitor() {
    private var count = 0

    fun report(): String? = if (count > 0) "$count common statics imported" else null

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
                count ++
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
                    count ++
                }
            })
        }
    }
}
