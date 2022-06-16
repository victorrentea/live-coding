package com.github.victorrentea.livecoding.ux

import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener

class TestListener : TestStatusListener() {

    override fun testSuiteFinished(root: AbstractTestProxy?) {
        if (root==null) return
//        println("Status : passed: " + root.hasPassedTests())
//        println("Status : defect: " + root.isDefect)
//        println("Status : interrupted: " + root.isInterrupted)
    }
}