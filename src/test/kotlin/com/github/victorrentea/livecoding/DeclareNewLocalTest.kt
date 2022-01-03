package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.declarenewlocal.DeclareNewLocalFix
import com.github.victorrentea.livecoding.declarenewlocal.DeclareNewLocalInspection
import com.intellij.codeInspection.LocalInspectionTool
import org.junit.runners.Parameterized


class DeclareNewLocalTest(fileName: String) : InspectionParameterizedTestBase(fileName) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("declarenewlocal")
    }

    override fun inspectionName() = DeclareNewLocalInspection.INSPECTION_NAME
    override fun fixName() = DeclareNewLocalFix.FIX_NAME

    override fun inspectionClass(): Class<out LocalInspectionTool> = DeclareNewLocalInspection::class.java


}