package com.github.victorrentea.livecoding.ux

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.io.BufferedInputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Plays a pass/fail sound and shows a splash when a test run finishes.
 *
 * Subscribes to the SM test-runner message-bus topic (`SMTRunnerEventsListener.TEST_STATUS`) via
 * `<projectListeners>`, instead of the `com.intellij.testStatusListener` extension point. That EP
 * is non-dynamic on 2024.3 / 2025.1 and was the only thing forcing an IDE restart when the plugin
 * is installed or updated; a message-bus listener is dynamic on all supported builds.
 */
class TestListener : SMTRunnerEventsAdapter() {
    companion object {
        private val log = logger<TestListener>()
    }

    override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
        val settings = AppSettingsState.getInstance()
        val passed = testsRoot.isPassed || testsRoot.isIgnored

        if (settings.playTestResultsSound) {
            playSound(if (passed) "pass.wav" else "fail2.wav")
        }
        if (settings.showTestResultsSplash) {
            // UI must run on the EDT; the SM runner may notify from a background thread.
            ApplicationManager.getApplication().invokeLater {
                FadingOutSplash(if (passed) "pass" else "fail")
            }
        }
    }

    private fun playSound(fileName: String) {
        TestListener::class.java.getResourceAsStream("/icons/$fileName").use {
            if (it == null) {
                log.error("Cannot open audio stream: $fileName")
                return
            }
            val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(BufferedInputStream(it))
            val clip: Clip = AudioSystem.getClip()
            clip.open(audioInputStream)
            clip.start()
        }
    }
}
