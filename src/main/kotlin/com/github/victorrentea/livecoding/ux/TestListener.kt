package com.github.victorrentea.livecoding.ux

import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener
import javax.swing.Timer

class TestListener : TestStatusListener() {
    override fun testSuiteFinished(root: AbstractTestProxy?) {
        if (root == null) return

        if (root.hasPassedTests()) {
            BackgroundImageUtil.setBackgroundImageFadingOut(BackgroundImage.GREEN, 50,10)
        } else {
            BackgroundImageUtil.setBackgroundImageFadingOut(BackgroundImage.RED, 30, 10)
        }

//        println("Status : passed: " + root.hasPassedTests())
//        println("Status : defect: " + root.isDefect)
//        println("Status : interrupted: " + root.isInterrupted)
    }
}