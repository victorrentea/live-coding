package com.github.victorrentea.livecoding.tokens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenCounterTest {

    @Test
    fun countsKnownTokenizationExactly() {
        // "hello world" is 2 tokens in every OpenAI BPE vocabulary
        assertEquals(2, TokenCounter.count("hello world", TokenizerFamily.GPT))
        assertEquals(2, TokenCounter.count("hello world", TokenizerFamily.GPT_LEGACY))
    }

    @Test
    fun emptyTextHasNoTokens() {
        assertEquals(0, TokenCounter.count("", TokenizerFamily.GPT))
        assertEquals(0, TokenCounter.tokenize("", TokenizerFamily.GPT).count)
    }

    @Test
    fun approximateFamiliesScaleTheExactCount() {
        val text = "public class Order { private final List<OrderLine> lines = new ArrayList<>(); }"
        val exact = TokenCounter.count(text, TokenizerFamily.GPT_LEGACY)
        assertEquals(Math.round(exact * 1.15).toInt(), TokenCounter.count(text, TokenizerFamily.CLAUDE))
    }

    @Test
    fun tokenBoundariesCoverTheWholeTextInOrder() {
        val text = "public class Order {\n    private String name; // the customer's name\n}\n"
        assertBoundariesTile(text)
    }

    @Test
    fun tokenBoundariesSurviveMultiByteCharacters() {
        // Cyrillic + emoji: several UTF-8 bytes per character, and tokens may split inside one
        assertBoundariesTile("// ăâîșț привет 🚀 done\nval x = \"日本語\"\n")
    }

    /** Ranges must be non-decreasing, start at 0 and end at the text end - i.e. tile the document. */
    private fun assertBoundariesTile(text: String) {
        for (family in TokenizerFamily.values()) {
            val tokenization = TokenCounter.tokenize(text, family)
            val starts = tokenization.starts
            assertTrue("$family produced no tokens", starts.isNotEmpty())
            assertEquals("$family must start at offset 0", 0, starts.first())
            assertEquals("$family must end at the text end", text.length, tokenization.end)
            for (k in 1 until starts.size) {
                assertTrue(
                    "$family boundary $k went backwards: ${starts[k - 1]} -> ${starts[k]}",
                    starts[k] >= starts[k - 1]
                )
                assertTrue("$family boundary $k is past the text end", starts[k] <= text.length)
            }
        }
    }
}
