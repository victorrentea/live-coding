package com.github.victorrentea.livecoding

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
abstract class InspectionParameterizedTestBase(val fileName: String) : LightJavaCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = JAVA_8_ANNOTATED

    override fun getTestDataPath() = JAVA_SRC_FOLDER

    protected fun HighlightInfo.getLineNumber() = getLineNumber(myFixture.file, actualStartOffset)

    protected abstract fun quickfixName(): String

    protected abstract fun inspectionClass(): Class<out LocalInspectionTool>

    @Test
    fun test() {
        myFixture.enableInspections(inspectionClass())

        myFixture.configureByFiles(fileName);

        val expectedHighlightedLines = File(JAVA_SRC_FOLDER, fileName).readLines()
            .mapIndexedNotNull { lineNumber, line -> if (line.contains("//")) lineNumber + 1 else null }
        println("EXPECTED HIGHLIGHTED LINES: " + expectedHighlightedLines)

        val highlights = myFixture.doHighlighting()
            /*.also { println("Highlights: " + it) }.*/
            .filter { it.description == quickfixName() }

        val actualHighlightedLines = highlights.map { it.getLineNumber() }

        assertEquals("Highlighted line numbers", actualHighlightedLines, expectedHighlightedLines)

        if (highlights.isNotEmpty()) {
            val highlight = highlights[0]

            myFixture.editor.caretModel.moveToOffset(highlight!!.startOffset)

            val intention = myFixture.getAvailableIntention(quickfixName())!!
            println("TEST: applying fix at line ${highlight.getLineNumber()}")
            intention.invoke(project, editor, file)

            val expectedFile = File(javaSrcFolder, getExpectedFile(fileName))
            if (!expectedFile.isFile) {
                throw IllegalArgumentException("Expected file not found at ${expectedFile.absolutePath}")
            }
            myFixture.checkResultByFile(getExpectedFile(fileName));
        }
    }
}
