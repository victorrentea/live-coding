package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.github.victorrentea.livecoding.settings.SettingsConfigurable
import javax.swing.Icon

/**
 * Status bar widget: "2,500 tokens (GPT)". Clicking it opens the model picker and the highlighting toggle.
 */
class TokenCountWidget(project: Project) :
    EditorBasedWidget(project),
    StatusBarWidget.MultipleTextValuesPresentation {

    private val service get() = TokenCountService.getInstance(project)

    override fun ID() = ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)
        service.refresh()
    }

    override fun getSelectedValue() = service.display?.text ?: "-- tokens"

    /** Two coins: the widget counts what an LLM call will be billed for, so it is money, not paint. */
    override fun getIcon(): Icon = COINS

    override fun getTooltipText() = service.display?.tooltip ?: "LLM token count of the current file"

    override fun getPopup(): JBPopup {
        val group = DefaultActionGroup()
        TokenizerFamily.values().forEach { group.add(SelectFamilyAction(it)) }
        group.addSeparator()
        group.add(ToggleHighlightingAction())
        group.addSeparator()
        group.add(OpenSettingsAction())
        return JBPopupFactory.getInstance().createActionGroupPopup(
            "Count Tokens For",
            group,
            SimpleDataContext.getProjectContext(project),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
    }

    private inner class SelectFamilyAction(private val family: TokenizerFamily) :
        DumbAwareToggleAction(family.displayName, family.note, null) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(e: AnActionEvent) = service.family == family
        override fun setSelected(e: AnActionEvent, state: Boolean) {
            service.family = family
        }
    }

    private inner class ToggleHighlightingAction : DumbAwareToggleAction(
        "Highlight Token Boundaries",
        "Paint alternating backgrounds over each token in the editor",
        null
    ) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(e: AnActionEvent) = service.highlightTokens
        override fun setSelected(e: AnActionEvent, state: Boolean) {
            service.highlightTokens = state
        }
    }

    private inner class OpenSettingsAction :
        DumbAwareAction("Token Counter Settings...", "Open the Live-Coding settings", null) {
        override fun actionPerformed(e: AnActionEvent) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable::class.java)
        }
    }

    companion object {
        const val ID = "LiveCoding.TokenCount"

        /** `coins_dark.svg` sits next to it; the platform picks the variant per theme. */
        private val COINS: Icon = IconLoader.getIcon("/icons/coins.svg", TokenCountWidget::class.java)
    }
}
