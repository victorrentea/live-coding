package com.github.victorrentea.livecoding.tokens

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.IntArrayList
import java.util.concurrent.ConcurrentHashMap

/** Token boundaries as character offsets in the source text: token *k* spans `[starts[k], starts[k+1])`. */
class Tokenization(val count: Int, val starts: IntArray, val end: Int)

/**
 * Counts tokens - and locates their boundaries in the text - with the bundled BPE vocabularies.
 *
 * Vocabularies are loaded lazily (each is a multi-MB table) and cached for the IDE's lifetime.
 */
object TokenCounter {
    /** Above this, we count but never map boundaries: the offset table would cost more than the answer. */
    const val MAX_CHARS_FOR_BOUNDARIES = 200_000

    private val registry by lazy { Encodings.newLazyEncodingRegistry() }
    private val encodings = ConcurrentHashMap<TokenizerFamily, Encoding>()

    private fun encoding(family: TokenizerFamily): Encoding =
        encodings.computeIfAbsent(family) { registry.getEncoding(it.encodingType) }

    fun count(text: String, family: TokenizerFamily): Int =
        calibrate(encoding(family).countTokensOrdinary(text), family)

    /**
     * Tokenizes and maps every token back to a character range.
     *
     * JTokkit only returns token ids, so boundaries are recovered by decoding each token back to its
     * UTF-8 bytes and walking a byte-offset -> char-offset table built from the text.
     */
    fun tokenize(text: String, family: TokenizerFamily): Tokenization {
        val ids = encoding(family).encodeOrdinary(text)
        if (text.isEmpty() || ids.size() == 0) {
            return Tokenization(calibrate(ids.size(), family), IntArray(0), 0)
        }
        val byteToChar = byteToCharTable(text)
        val enc = encoding(family)
        val single = IntArrayList(1)
        val starts = IntArray(ids.size())
        var byteOffset = 0
        for (k in 0 until ids.size()) {
            starts[k] = byteToChar[byteOffset.coerceAtMost(byteToChar.size - 1)]
            single.clear()
            single.add(ids.get(k))
            byteOffset += enc.decodeBytes(single).size
        }
        return Tokenization(calibrate(ids.size(), family), starts, text.length)
    }

    private fun calibrate(rawCount: Int, family: TokenizerFamily) =
        if (family.approximate) Math.round(rawCount * family.calibration).toInt() else rawCount

    /**
     * `table[b]` = index of the character whose UTF-8 encoding contains byte `b`; the last entry is
     * `text.length`, so a token ending on the final byte maps to the end of the text.
     */
    private fun byteToCharTable(text: String): IntArray {
        var byteCount = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            byteCount += utf8Length(cp)
            i += Character.charCount(cp)
        }
        val table = IntArray(byteCount + 1)
        var b = 0
        var c = 0
        while (c < text.length) {
            val cp = text.codePointAt(c)
            repeat(utf8Length(cp)) { table[b++] = c }
            c += Character.charCount(cp)
        }
        table[byteCount] = text.length
        return table
    }

    private fun utf8Length(codePoint: Int) = when {
        codePoint < 0x80 -> 1
        codePoint < 0x800 -> 2
        codePoint < 0x10000 -> 3
        else -> 4
    }
}
