package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.Constants.DECLARE_NEW_LOCAL_FIX_NAME
import com.github.victorrentea.livecoding.Constants.DECLARE_NEW_LOCAL_INSPECTION_NAME
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

    override fun inspectionName() = DECLARE_NEW_LOCAL_INSPECTION_NAME
    override fun fixName() = DECLARE_NEW_LOCAL_FIX_NAME

    override fun inspectionClass(): Class<out LocalInspectionTool> = DeclareNewLocalInspection::class.java


}