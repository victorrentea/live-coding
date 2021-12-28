package com.github.victorrentea.livecoding

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CognitiveComplexityTest(private val fileName: String) : LightJavaCodeInsightFixtureTestCase() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<String> = getInputFilePaths("cognitive")
    }

    override fun getProjectDescriptor() = JAVA_8

    override fun getTestDataPath() = JAVA_SRC_FOLDER

    @Test
    fun test() {
        myFixture.configureByFiles(fileName);

        val file = myFixture.file as PsiJavaFile
        println("FILE: " + file)
        assert(file.classes.size == 1)
        val psiClass = file.classes[0]
        for (method in psiClass.methods) {
            println("CHECKING METHOD " + method.name)
            val comment = PsiTreeUtil.findChildOfType(method, PsiComment::class.java)
            if (comment == null) throw AssertionError("Missing comment on method ${method.name}")
            if (!comment.text.matches(Regex("\\W*//\\W*\\d+\\W*"))) throw AssertionError("Method ${method.name} expected to be commented with // <x>  (x = expected cognitive complexity). Found: \"" + comment.text + "\"")
            val expectedComplexity = comment.text.substringAfter("//").trim().toInt()

            val visitor = CognitiveComplexityVisitor()
            visitor.visitElement(method)
            TestCase.assertEquals("Complexity of '${method.name}'", expectedComplexity, visitor.complexity)
        }
    }
}
