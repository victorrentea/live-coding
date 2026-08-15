package com.github.victorrentea.livecoding.tokens

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Alarm
import java.text.NumberFormat

/** What the status bar widget currently shows, and why (tooltip). */
data class TokenCountDisplay(val text: String, val tooltip: String)

/**
 * Recomputes the token count of the selected editor - and, when enabled, the per-token highlighting -
 * shortly after every edit, caret move or editor switch.
 *
 * Tokenizing runs on a pooled thread against a text snapshot: it is far too slow to do on every
 * keystroke on the EDT.
 */
class TokenCountService(private val project: Project) : Disposable {

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val numberFormat = NumberFormat.getIntegerInstance()

    @Volatile
    var display: TokenCountDisplay? = null
        private set

    var family: TokenizerFamily
        get() = settings.tokenizerFamily
        set(value) {
            settings.tokenizerFamily = value
            refresh()
        }

    var highlightTokens: Boolean
        get() = settings.highlightTokens
        set(value) {
            settings.highlightTokens = value
            if (!value) TokenHighlighter.clearAll()
            refresh()
        }

    private val settings get() = AppSettingsState.getInstance()

    init {
        val multicaster = EditorFactory.getInstance().eventMulticaster
        multicaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = scheduleUpdate()
        }, this)
        multicaster.addSelectionListener(object : SelectionListener {
            override fun selectionChanged(event: SelectionEvent) = scheduleUpdate()
        }, this)
        project.messageBus.connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = refresh()
            })
    }

    /** Recompute now (settings changed, editor switched) rather than after the typing delay. */
    fun refresh() = scheduleUpdate(delayMs = 0)

    private fun scheduleUpdate(delayMs: Int = TYPING_DELAY_MS) {
        if (project.isDisposed) return
        alarm.cancelAllRequests()
        alarm.addRequest({ update() }, delayMs)
    }

    private fun update() {
        if (project.isDisposed) return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            publish(null, null, null)
            return
        }
        // Snapshot on the EDT; everything below runs against these immutable copies.
        val text = editor.document.immutableCharSequence.toString()
        val selection = editor.selectionModel.selectedText
        val family = family
        val wantHighlight = highlightTokens && text.length <= TokenCounter.MAX_CHARS_FOR_BOUNDARIES
        val modificationStamp = editor.document.modificationStamp

        ApplicationManager.getApplication().executeOnPooledThread {
            val tokenization = if (wantHighlight) TokenCounter.tokenize(text, family) else null
            val total = tokenization?.count ?: TokenCounter.count(text, family)
            val selected = selection?.takeIf { it.isNotEmpty() }?.let { TokenCounter.count(it, family) }
            ApplicationManager.getApplication().invokeLater({
                if (project.isDisposed || editor.isDisposed) return@invokeLater
                // A newer edit already landed - its own update is on the way, don't paint stale ranges.
                if (editor.document.modificationStamp != modificationStamp) return@invokeLater
                publish(total, selected, family)
                if (wantHighlight && tokenization != null) TokenHighlighter.apply(editor, tokenization)
                else TokenHighlighter.clear(editor)
            }, project.disposed)
        }
    }

    private fun publish(total: Int?, selected: Int?, family: TokenizerFamily?) {
        display = when {
            total == null || family == null -> null
            selected != null -> TokenCountDisplay(
                "${prefix(family)}${numberFormat.format(selected)} / ${numberFormat.format(total)} tok (${family.shortLabel})",
                "Tokens in the selection / in the whole file.\n${family.displayName}: ${family.note}"
            )

            else -> TokenCountDisplay(
                "${prefix(family)}${numberFormat.format(total)} tok (${family.shortLabel})",
                "Tokens in this file.\n${family.displayName}: ${family.note}\nClick to switch model or toggle highlighting."
            )
        }
        WindowManager.getInstance().getStatusBar(project)?.updateWidget(TokenCountWidget.ID)
    }

    private fun prefix(family: TokenizerFamily) = if (family.approximate) "~" else ""

    override fun dispose() = TokenHighlighter.clearAll()

    companion object {
        private const val TYPING_DELAY_MS = 400

        fun getInstance(project: Project): TokenCountService = project.getService(TokenCountService::class.java)
    }
}
