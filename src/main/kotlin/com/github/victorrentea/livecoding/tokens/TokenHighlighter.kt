package com.github.victorrentea.livecoding.tokens

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Paints alternating background stripes over each token, so an audience can *see* where the model
 * splits the code. Only the background is set, so syntax colouring stays intact.
 */
object TokenHighlighter {
    private val OURS = Key.create<List<RangeHighlighter>>("LiveCoding.TokenHighlighters")

    private val EVEN = TextAttributes().apply {
        backgroundColor = JBColor(Color(0xD3, 0xE2, 0xFF), Color(0x2C, 0x3B, 0x55))
    }
    private val ODD = TextAttributes().apply {
        backgroundColor = JBColor(Color(0xFF, 0xE3, 0xB8), Color(0x4B, 0x3B, 0x22))
    }

    fun apply(editor: Editor, tokenization: Tokenization) {
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
                start, end,
                HighlighterLayer.ADDITIONAL_SYNTAX,
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
