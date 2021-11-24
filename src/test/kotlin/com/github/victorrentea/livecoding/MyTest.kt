package com.github.victorrentea.livecoding

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil

class MyTest : BasePlatformTestCase() {

    override fun getTestDataPath() = "src/test/testData"


    fun testJavaFile() {
        val psiFile = assertInstanceOf(myFixture.configureByFile("One.java"), PsiJavaFile::class.java)

        println(psiFile)

    }

}
