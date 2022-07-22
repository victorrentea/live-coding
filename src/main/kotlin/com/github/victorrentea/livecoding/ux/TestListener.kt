package com.github.victorrentea.livecoding.ux

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo.Magnitude
import com.intellij.openapi.diagnostic.logger
import java.io.BufferedInputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class TestListener : TestStatusListener() {
    companion object {
        protected val log = logger<TestStatusListener>()
    }
    override fun testSuiteFinished(root: AbstractTestProxy?) {
        if (root == null) return


        val passed = root.magnitude == Magnitude.PASSED_INDEX.value
                ||root.magnitude == Magnitude.IGNORED_INDEX.value

        if (passed) {
            if (AppSettingsState.getInstance().playTestResultsSound)
                playSound("pass.wav");
            if (AppSettingsState.getInstance().showTestResultsSplash)
                FadingOutSplash("pass")
        } else {
            if (AppSettingsState.getInstance().playTestResultsSound)
                playSound("fail2.wav");
            if (AppSettingsState.getInstance().showTestResultsSplash)
                FadingOutSplash("fail")
        }
//        println("Status : passed: " + root.hasPassedTests())
//        println("Status : defect: " + root.isDefect)
//        println("Status : interrupted: " + root.isInterrupted)
    }
    private fun playSound(fileName: String) {
        TestListener::class.java.getResourceAsStream("/icons/$fileName").use {
            if (it==null) {
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