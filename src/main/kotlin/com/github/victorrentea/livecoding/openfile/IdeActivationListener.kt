package com.github.victorrentea.livecoding.openfile

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame

/**
 * Tracks which IDE window currently has OS focus. Focusing a window (re)starts the dwell on
 * whatever file is already selected there — so staring at an already-open file for 5s reports it,
 * even without a tab switch. Registered via {@code <applicationListeners>} (dynamic-unload-safe).
 */
class IdeActivationListener : ApplicationActivationListener {
    override fun applicationActivated(ideFrame: IdeFrame) {
        OpenFileReporter.getInstance().ideActivated(ideFrame.project)
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
        OpenFileReporter.getInstance().ideDeactivated()
    }
}
