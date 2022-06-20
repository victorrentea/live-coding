package com.github.victorrentea.livecoding.ux

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener
import javax.swing.Timer

class TestListener : TestStatusListener() {
    override fun testSuiteFinished(root: AbstractTestProxy?) {
        if (root == null) return

        if (!AppSettingsState.getInstance().showTestResultsSplash) return;

        if (root.isPassed)
            FadingOutSplash("pass")
        else
            FadingOutSplash("fail")
//        println("Status : passed: " + root.hasPassedTests())
//        println("Status : defect: " + root.isDefect)
//        println("Status : interrupted: " + root.isInterrupted)
    }
}