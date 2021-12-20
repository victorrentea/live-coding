package com.github.victorrentea.livecoding

import com.intellij.codeInspection.LocalInspectionTool
import org.junit.Ignore
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@Ignore // how to add lombok to virtual project ?
class LombokAddRacTest(fileName: String) : InspectionParameterizedTestBase(fileName) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("lombok.addrac")
    }

    override fun inspectionName() = AddRequiredArgsConstructorInspection.INSPECTION_NAME
    override fun fixName() = AddRequiredArgsConstructorFix.FIX_NAME

    override fun inspectionClass(): Class<out LocalInspectionTool> = AddRequiredArgsConstructorInspection::class.java


}