package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.lombok.AddRequiredArgsConstructorInspection
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.rootManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.MavenDependencyUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
abstract class InspectionParameterizedTestBase(private val fileName: String) : LightJavaCodeInsightFixtureTestCase() {
    private val log = logger<InspectionParameterizedTestBase>()
    override fun getProjectDescriptor() = JAVA_8

    override fun getTestDataPath() = JAVA_SRC_FOLDER

    private fun HighlightInfo.getLineNumber() = getLineNumber(myFixture.file, actualStartOffset)

    protected abstract fun inspectionName(): String
    protected abstract fun fixName(): String

    protected abstract fun inspectionClass(): Class<out LocalInspectionTool>


    @Test
    fun test() {

        myFixture.enableInspections(inspectionClass())

        myFixture.configureByFiles(fileName)

        val expectedHighlightedLines = File(JAVA_SRC_FOLDER, fileName).readLines()
            .mapIndexedNotNull { lineNumber, line -> if (line.contains("//")) lineNumber + 1 else null }
        log.info("EXPECTED HIGHLIGHTED LINES: $expectedHighlightedLines")

        val highlights = myFixture.doHighlighting()
            .also { allHighlights ->
                log.info("-- all highlights --")
                allHighlights.forEach { log.info("${it.getLineNumber()}: ${it.description}") }
            }
            .filter { it.description == inspectionName() }

        log.info("Got ${highlights.size} matches by name")

        val actualHighlightedLines = highlights.map { it.getLineNumber() }

        assertEquals("Highlighted line numbers", expectedHighlightedLines, actualHighlightedLines)

        if (highlights.isNotEmpty()) {
            val highlight = highlights[0]

            myFixture.editor.caretModel.moveToOffset(highlight!!.startOffset)

            val intention = myFixture.getAvailableIntention(fixName())!!
            println("TEST: applying fix at line ${highlight.getLineNumber()}")
            intention.invoke(project, editor, file)

            val expectedFile = File(javaSrcFolder, getExpectedFile(fileName))
            if (!expectedFile.isFile) {
                throw IllegalArgumentException("Expected file not found at ${expectedFile.absolutePath}")
            }
            myFixture.checkResultByFile(getExpectedFile(fileName))
        }
    }
}
