package com.github.victorrentea.livecoding

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class MyPluginParameterizedTest(val fileName: String)  : LightJavaCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = JAVA_8_ANNOTATED

    override fun getTestDataPath() = JAVA_SRC_FOLDER

    companion object {
        const val JAVA_SRC_FOLDER = "src/test/live-coding-playground/src/main/java"
        @JvmStatic
        @Parameterized.Parameters(name="{0}")
        fun parameters(): List<String> {
//            return listOf<String>("splitvariable/AlternateBlocks")
            val javaSrcFolder = File(JAVA_SRC_FOLDER)
            val folder = File(javaSrcFolder, "splitvariable")
            if (!folder.exists()) throw IllegalArgumentException("Folder does not exist: ${folder.absolutePath}")
            val files = folder.walkTopDown().filter { it.isFile }.toList()
            if (files.isEmpty()) throw IllegalArgumentException("No files found in ${folder.absolutePath}")
            val names = files.map { it.toRelativeString(javaSrcFolder) }
            for (inputFileName in names) {
                val expectedFile = File(javaSrcFolder, getExpectedFile(inputFileName))
                if (!expectedFile.isFile)
                    throw IllegalArgumentException("Expected file not found at ${expectedFile.absolutePath}")
            }
            println("FOUND NAMES: $names")
            return names
        }
    private fun getExpectedFile(inputFileName:String) = "../../test/java/$inputFileName"
    }


    @Test
    fun test() {
        myFixture.enableInspections(SplitVariableInspection::class.java)

        myFixture.configureByFiles(fileName);

        val highlight = myFixture.doHighlighting().find { it.description == Constants.SPLIT_VARIABLE_DESCRIPTION}

        if (highlight == null) fail("Intention not found in file")

        myFixture.editor.caretModel.moveToOffset(highlight!!.startOffset)

        val intention = myFixture.getAvailableIntention(Constants.SPLIT_VARIABLE_DESCRIPTION) !!

        intention.invoke(project, editor, file)

        myFixture.checkResultByFile(getExpectedFile(fileName));
    }

}