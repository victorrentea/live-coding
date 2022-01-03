package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.lombok.ReplaceRequiredArgsConstructorFix
import com.github.victorrentea.livecoding.lombok.ReplaceRequiredArgsConstructorInspection
import com.intellij.codeInspection.LocalInspectionTool
import org.junit.Ignore
import org.junit.runners.Parameterized.Parameters

@Ignore // how to add lombok to virtual project ?
class LombokReplaceRacTest(fileName: String) : InspectionParameterizedTestBase(fileName) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("lombok\\replacerac")
    }

    override fun inspectionName() = ReplaceRequiredArgsConstructorInspection.INSPECTION_NAME
    override fun fixName() = ReplaceRequiredArgsConstructorFix.FIX_NAME

    override fun inspectionClass(): Class<out LocalInspectionTool> = ReplaceRequiredArgsConstructorInspection::class.java


}