package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.Constants.SPLIT_VARIABLE_DESCRIPTION
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File


class DeclareNewLocalTest(fileName: String) : InspectionParameterizedTestBase(fileName) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("declarenewlocal")
    }

    override fun quickfixName() = SPLIT_VARIABLE_DESCRIPTION

    override fun inspectionClass(): Class<out LocalInspectionTool> = SplitVariableInspection::class.java


}