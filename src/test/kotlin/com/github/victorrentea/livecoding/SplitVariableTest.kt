package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.Constants.SPLIT_VARIABLE_DESCRIPTION
import com.intellij.codeInspection.LocalInspectionTool
import org.junit.Ignore
import org.junit.runners.Parameterized


@Ignore
class SplitVariableTest(fileName: String) : InspectionParameterizedTestBase(fileName) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("splitlocal")
    }

    override fun inspectionName() = SPLIT_VARIABLE_DESCRIPTION
    override fun fixName() = SPLIT_VARIABLE_DESCRIPTION

    override fun inspectionClass(): Class<out LocalInspectionTool> = SplitVariableInspection::class.java


}