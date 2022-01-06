package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ImportUtils


class ImportStaticsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val visitor = ImportStaticsVisitor()

        // do work
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        psiFile?.accept(visitor)

        // visual notification
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        ApplicationManager.getApplication().invokeLater {
            visitor.report()?.let { reportString ->
                HintManager.getInstance().showInformationHint(editor, reportString)
            }
        }
    }
    class ImportStaticsVisitor : PsiRecursiveElementWalkingVisitor() {
        private var count = 0
        private val classes = mutableSetOf<String>()

        fun report(): String? = if (count == 0) "Auto-import statics: nothing to do" else "$count common statics imported from:\n" +
                classes.sorted().joinToString("\n") { "- $it"}

        override fun visitElement(element: PsiElement) {
            super.visitElement(element)

            if (element !is PsiReferenceExpression) return
            if (!AppSettingsState.getInstance().staticImports.containsKey(element.referenceName)) return
            if (element.qualifierExpression != null) {
                replaceWithStaticImport(element)
            } else {
                addStaticImportForUnresolvedReference(element)
            }
        }

        private fun addStaticImportForUnresolvedReference(unresolvedReference: PsiReferenceExpression) {
            if (unresolvedReference.resolve() != null) return// unqualified method not resolved
            val methodName = unresolvedReference.referenceName ?: return
            val desiredClassQName = AppSettingsState.getInstance().staticImports[methodName] ?: return

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(unresolvedReference.project, "Auto-Import Statics", "Live-Coding", {
                    ImportUtils.addStaticImport(desiredClassQName, methodName, unresolvedReference)
                    count ++
                    classes += desiredClassQName
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
                        classes += desiredClassQName
                    }
                })
            }
        }
    }
}
