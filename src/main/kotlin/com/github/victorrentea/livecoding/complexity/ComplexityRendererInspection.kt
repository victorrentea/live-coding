package com.github.victorrentea.livecoding.complexity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.refactoring.suggested.startOffset
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor

class ComplexityRendererInspection : BaseInspection() {
    companion object {
        private val log = logger<ComplexityRendererInspection>()
    }

    override fun buildErrorString(vararg infos: Any?) = "Inspecting method complexity"

    override fun buildVisitor() = ComplexityVisitor()

    class ComplexityVisitor : BaseInspectionVisitor() {
        override fun visitMethod(method: PsiMethod?) {
            if (method == null) return
            val methodBody = method.body ?: return
            val totalComplexity = CognitiveComplexityVisitor().visitElement(method).total()
            log.debug("Method complexity : $totalComplexity")

            ApplicationManager.getApplication().invokeLater {
                PsiEditorUtil.findEditor(method)?.markupModel?.let { markupModel ->
                    val h: RangeHighlighter = markupModel.addRangeHighlighter(
                        null,
                        method.startOffset,
                        methodBody.startOffset + 1,
                        HighlighterLayer.LAST - 1,
                        HighlighterTargetArea.LINES_IN_RANGE
                    )

                    h.customRenderer = ComplexityRenderer(totalComplexity)
                }
            }
        }
    }
}