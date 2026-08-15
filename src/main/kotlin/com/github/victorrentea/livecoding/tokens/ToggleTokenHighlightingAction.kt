package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

/** Bindable action so the token stripes can be flipped on/off mid-demo without hunting the status bar. */
class ToggleTokenHighlightingAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = e.project != null
    }

    override fun isSelected(e: AnActionEvent) =
        e.project?.let { TokenCountService.getInstance(it).highlightTokens } ?: false

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.let { TokenCountService.getInstance(it).highlightTokens = state }
    }
}
