package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.Constants.SPLIT_VARIABLE_DESCRIPTION
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class MyPluginParameterizedTest(val fileName: String) : LightJavaCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = JAVA_8_ANNOTATED

    override fun getTestDataPath() = JAVA_SRC_FOLDER

    companion object {
        const val JAVA_SRC_FOLDER = "src/test/live-coding-playground/src/main/java"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<String> {
//            return listOf<String>("splitvariable/AlternateBlocks")
            val javaSrcFolder = File(JAVA_SRC_FOLDER)
            val folder = File(javaSrcFolder, "splitvariable")
            if (!folder.exists()) throw IllegalArgumentException("Folder does not exist: ${folder.absolutePath}")
            val files = folder.walkTopDown().filter { it.isFile }.toList()
            if (files.isEmpty()) throw IllegalArgumentException("No files found in ${folder.absolutePath}")
            val names = files.map { it.toRelativeString(javaSrcFolder) }
            for (inputFileName in names) {
//                val expectedFile = File(javaSrcFolder, getExpectedFile(inputFileName))
//                if (!expectedFile.isFile)
//                    throw IllegalArgumentException("Expected file not found at ${expectedFile.absolutePath}")
            }
            println("FOUND NAMES: $names")
            return names
        }

        private fun getExpectedFile(inputFileName: String) = "../../test/java/$inputFileName"
    }


    @Test
    fun test() {
        myFixture.enableInspections(SplitVariableInspection::class.java)

        myFixture.configureByFiles(fileName);


        val expectedHighlightedLines = File(JAVA_SRC_FOLDER, fileName).readLines()
            .mapIndexedNotNull { lineNumber, line -> if (line.contains("//")) lineNumber + 1 else null }
        println("EXPECTED HIGHLIGHTED LINES: " + expectedHighlightedLines)

        val highlights = myFixture.doHighlighting()./*also { println("Highlights: " + it) }.*/filter { it.description == SPLIT_VARIABLE_DESCRIPTION }

        val actualHighlightedLines = highlights.map { it.getLineNumber() }

        println("Actual highlighted lines: " + actualHighlightedLines + "vs" + expectedHighlightedLines)
        assertEquals("Highlighted line numbers", actualHighlightedLines, expectedHighlightedLines)

        if (highlights.isNotEmpty()) {
            val highlight = highlights[0]

            myFixture.editor.caretModel.moveToOffset(highlight!!.startOffset)

            val intention = myFixture.getAvailableIntention(SPLIT_VARIABLE_DESCRIPTION)!!
            println("TEST: applying fix at line ${highlight.getLineNumber()}")
            intention.invoke(project, editor, file)

            myFixture.checkResultByFile(getExpectedFile(fileName));
        }
    }

    private fun HighlightInfo.getLineNumber()=
        getLineNumber(myFixture.file, actualStartOffset)

}