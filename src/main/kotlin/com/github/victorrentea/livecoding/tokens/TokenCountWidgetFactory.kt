package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

class TokenCountWidgetFactory : StatusBarEditorBasedWidgetFactory() {
    override fun getId() = TokenCountWidget.ID
    override fun getDisplayName() = "LLM Token Count"
    override fun createWidget(project: Project): StatusBarWidget = TokenCountWidget(project)
}
