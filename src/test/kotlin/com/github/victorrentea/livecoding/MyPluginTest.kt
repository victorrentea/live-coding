package com.github.victorrentea.livecoding

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase


class MyPluginTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor() = JAVA_8_ANNOTATED

    override fun getTestDataPath() = "src/test/live-coding-playground/src/main/java"


    fun testMyJava() {
        myFixture.enableInspections(SplitVariableInspection::class.java)

        myFixture.configureByFiles("splitvariable/AlternateBlocks.java");

        val highlight = myFixture.doHighlighting().find { it.description == Constants.SPLIT_VARIABLE_DESCRIPTION}

        if (highlight == null) fail("Intention not found in file")

        myFixture.editor.caretModel.moveToOffset(highlight!!.startOffset)

        val intention = myFixture.getAvailableIntention(Constants.SPLIT_VARIABLE_DESCRIPTION) !!

        intention.invoke(project, editor, file)

        myFixture.checkResultByFile("../../test/java/splitvariable/AlternateBlocks.java");
    }
}