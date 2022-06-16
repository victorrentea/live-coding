package com.github.victorrentea.livecoding.complexity

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.*
import com.intellij.ui.layout.panel
import javax.swing.JPanel

class CognitiveComplexityInlineHintsProvider: InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): FactoryInlayHintsCollector? {
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return null
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink) : Boolean {
                if (file.project.service<DumbService>().isDumb) return true

                val method = element as? PsiMethod ?: return true
                val methodName = method.nameIdentifier ?: return true;

                val project = file.project

                val cc = CognitiveComplexityVisitor().visitElement(method).total()

                val text = if (cc<=1) return true;
                    else if (cc<=5) "👌"
                    else if (cc<=10) "🥴 cognitive:$cc"
                    else "🛑 cognitive:$cc - REFACTOR!"
//                val text = "$emoji cognitive:$cc"

                val presentation =
                    factory.roundWithBackground(
                        factory.withTooltip(
                            "Cognitive Complexity is $cc",
                            factory.smallText(text)
                    )
                )

                val finalPresentation = InsetPresentation(MenuOnClickPresentation(presentation, project) {
                        val provider = this@CognitiveComplexityInlineHintsProvider
                        listOf(InlayProviderDisablingAction(provider.name, file.language, project, provider.key))
                    }, left = 1)
                sink.addInlineElement(methodName.textRange.endOffset, true, finalPresentation, true )
                return true
            }
        }
    }

    override val key: SettingsKey<NoSettings>
        get() = ourKey

    override fun createConfigurable(settings: NoSettings) = object : ImmediateConfigurable {

        override fun createComponent(listener: ChangeListener): JPanel {
            reset()
            val panel = panel {
            }
            return panel
        }

        override fun reset() {
        }
    }

    override fun createSettings() = NoSettings()

    override val name: String
        get() = "Cognitive Complexity Hint"

    override val previewText: String? = null

    companion object {
        val ourKey: SettingsKey<NoSettings> = SettingsKey("cognitive.complexity.settings")
    }
}