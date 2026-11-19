package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import java.awt.Color

/**
 * Paints alternating stripes over each token of the selection, so an audience can *see* where the
 * model splits the code.
 *
 * Same palette as the VS Code twin, in every theme: pastel blue / amber with near-black text. Dark
 * stripes under the theme's own syntax colours were unreadable, so the stripes set the text colour
 * too. They sit above the selection layer, otherwise the selection background would hide them -
 * and the selection is the only place they are ever painted.
 */
object TokenHighlighter {
    private val OURS = Key.create<List<RangeHighlighter>>("LiveCoding.TokenHighlighters")

    private val TEXT = Color(0x1F, 0x1F, 0x1F)
    private val EVEN = TextAttributes().apply {
        backgroundColor = Color(0xB8, 0xD4, 0xFF)
        foregroundColor = TEXT
    }
    private val ODD = TextAttributes().apply {
        backgroundColor = Color(0xFF, 0xE0, 0xA6)
        foregroundColor = TEXT
    }

    /** Above [HighlighterLayer.SELECTION], so the selection background doesn't paint over the stripes. */
    const val LAYER = HighlighterLayer.SELECTION + 1

    /**
     * Stripes [tokenization], which was computed over the document text starting at [baseOffset]
     * (the selection start), so its offsets are shifted by that much.
     */
    fun apply(editor: Editor, tokenization: Tokenization, baseOffset: Int = 0) {
        clear(editor)
        val starts = tokenization.starts
        if (starts.isEmpty()) return
        val markup = editor.markupModel
        val added = ArrayList<RangeHighlighter>(starts.size)
        for (k in starts.indices) {
            val start = starts[k]
            val end = if (k + 1 < starts.size) starts[k + 1] else tokenization.end
            if (end <= start) continue // token boundary fell inside a multi-byte character
            added += markup.addRangeHighlighter(
                baseOffset + start, baseOffset + end,
                LAYER,
                if (k % 2 == 0) EVEN else ODD,
                HighlighterTargetArea.EXACT_RANGE
            )
        }
        editor.putUserData(OURS, added)
    }

    fun clear(editor: Editor) {
        val previous = editor.getUserData(OURS) ?: return
        val markup = editor.markupModel
        previous.forEach { if (it.isValid) markup.removeHighlighter(it) }
        editor.putUserData(OURS, null)
    }

    /** Called on plugin/project teardown: never leave stripes behind on an editor we no longer track. */
    fun clearAll() = EditorFactory.getInstance().allEditors.forEach { clear(it) }
}
