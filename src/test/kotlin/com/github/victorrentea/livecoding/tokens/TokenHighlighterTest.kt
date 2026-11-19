package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import java.awt.Color

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

    fun testStripesOnlyTheSelectionAboveTheSelectionLayer() {
        myFixture.configureByText("Order.java", code)
        val editor = myFixture.editor
        val start = code.indexOf("private")
        val end = code.indexOf(";", start) + 1
        val tokenization = TokenCounter.tokenize(code.substring(start, end), TokenizerFamily.GPT)

        TokenHighlighter.apply(editor, tokenization, start)

        val ours = editor.markupModel.allHighlighters.sortedBy { it.startOffset }
        assertEquals(tokenization.count, ours.size)
        assertEquals(start, ours.first().startOffset)
        assertEquals(end, ours.last().endOffset)
        assertTrue(ours.all { it.layer > HighlighterLayer.SELECTION })
        // same palette as VS Code in every theme: dark text on pastel stripes
        assertTrue(ours.all { it.getTextAttributes(null)?.foregroundColor == Color(0x1F, 0x1F, 0x1F) })
    }

    fun testClearRemovesEveryHighlighter() {
        myFixture.configureByText("Order.java", code)
        val editor = myFixture.editor
        TokenHighlighter.apply(editor, TokenCounter.tokenize(editor.document.text, TokenizerFamily.GPT))

        TokenHighlighter.clear(editor)

        assertEmpty(editor.markupModel.allHighlighters.toList())
    }
}
