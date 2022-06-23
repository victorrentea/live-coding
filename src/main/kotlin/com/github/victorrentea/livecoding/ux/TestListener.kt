package com.github.victorrentea.livecoding.ux

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class TestListener : TestStatusListener() {
    override fun testSuiteFinished(root: AbstractTestProxy?) {
        if (root == null) return

        if (root.isPassed) {
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
            val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(it)
            val clip: Clip = AudioSystem.getClip()
            clip.open(audioInputStream)
            clip.start()
        }
    }
}