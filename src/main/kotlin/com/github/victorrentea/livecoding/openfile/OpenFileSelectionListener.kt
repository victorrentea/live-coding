package com.github.victorrentea.livecoding.openfile

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener

/**
 * Fires whenever the active file in a project changes (tab switch or fresh open).
 * Registered per-project via {@code <projectListeners>} (dynamic-unload-safe).
 */
class OpenFileSelectionListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        OpenFileReporter.getInstance().candidateChanged(event.manager.project, event.newFile)
    }
}
