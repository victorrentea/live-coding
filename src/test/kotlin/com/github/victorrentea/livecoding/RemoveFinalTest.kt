package com.github.victorrentea.livecoding

import com.intellij.codeInspection.LocalInspectionTool
import org.junit.Ignore
import org.junit.runners.Parameterized.Parameters

class RemoveFinalTest(fileName: String) : InspectionParameterizedTestBase(fileName) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("removefinal")
    }

    override fun inspectionName() = RemoveFinalFromLocalInspection.INSPECTION_NAME
    override fun fixName() = RemoveFinalFromLocalsFix.FIX_NAME

    override fun inspectionClass(): Class<out LocalInspectionTool> = RemoveFinalFromLocalInspection::class.java


}