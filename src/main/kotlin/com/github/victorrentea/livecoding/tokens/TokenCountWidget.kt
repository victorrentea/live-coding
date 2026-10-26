package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.ClickListener
import java.awt.event.MouseEvent
import javax.swing.JComponent

/**
 * Status bar widget: Clawd + the token count of the current file, e.g. "🦀 1,935" - the same look as
 * the token counter in the VS Code setup. A click toggles the per-token highlighting in the editor;
 * the model family lives in Settings › Tools › Live-Coding.
 */
class TokenCountWidget(private val project: Project) : CustomStatusBarWidget {

    private val service get() = TokenCountService.getInstance(project)

    private val panel = TextPanel.WithIconAndArrows().apply {
        icon = LiveCodingIcons.Clawd
    }

    private val clickListener = object : ClickListener() {
        override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
            if (event.button != MouseEvent.BUTTON1) return false
            service.highlightTokens = !service.highlightTokens
            return true
        }
    }

    init {
        clickListener.installOn(panel)
        refresh()
    }

    override fun ID() = ID

    override fun getComponent(): JComponent = panel

    override fun install(statusBar: StatusBar) {
        service.refresh()
    }

    /** Pull the latest count from the service into the label. Called by the service after each recount. */
    fun refresh() {
        val display = service.display
        panel.text = display?.text ?: "--"
        panel.toolTipText = display?.tooltip ?: "LLM token count of the current file"
        panel.revalidate()
        panel.repaint()
    }

    /** What the status bar shows right now - "1,935" or "12 / 1,935" when there is a selection. */
    val text: String get() = panel.text ?: ""

    override fun dispose() = clickListener.uninstall(panel)

    companion object {
        const val ID = "LiveCoding.TokenCount"
    }
}
