package com.github.victorrentea.livecoding.tokens

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class TokenHighlighterTest : LightJavaCodeInsightFixtureTestCase() {

    private val code = """
        package com.acme;
        class Order {
            private String customerName;
        }
    """.trimIndent()

    fun testHighlightsEveryTokenAndTilesTheDocument() {
        myFixture.configureByText("Order.java", code)
        val editor = myFixture.editor
        val tokenization = TokenCounter.tokenize(editor.document.text, TokenizerFamily.GPT)

        TokenHighlighter.apply(editor, tokenization)

        val ours = editor.markupModel.allHighlighters
            .filter { it.getTextAttributes(null)?.backgroundColor != null }
            .sortedBy { it.startOffset }
        assertEquals(tokenization.count, ours.size)
        assertEquals(0, ours.first().startOffset)
        assertEquals(editor.document.textLength, ours.last().endOffset)
        // consecutive tokens must touch, never overlap or leave a gap
        ours.zipWithNext { left, right -> assertEquals(left.endOffset, right.startOffset) }
    }

    fun testClearRemovesEveryHighlighter() {
        myFixture.configureByText("Order.java", code)
        val editor = myFixture.editor
        TokenHighlighter.apply(editor, TokenCounter.tokenize(editor.document.text, TokenizerFamily.GPT))

        TokenHighlighter.clear(editor)

        assertEmpty(editor.markupModel.allHighlighters.toList())
    }
}
