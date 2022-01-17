package com.github.victorrentea.livecoding.extracthints

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.scale.JBUIScale.scale
import java.awt.*

class ExtractSuggestionRenderer(private val depth: Int) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        g.color = Color.blue
        val sectionTextStartPoint =
            editor.logicalPositionToXY(editor.offsetToLogicalPosition(highlighter.startOffset))
        val sectionTextEndPoint =
            editor.logicalPositionToXY(editor.offsetToLogicalPosition(highlighter.endOffset))
                .also { it.x = sectionTextStartPoint.x }

        val hanging = depth * scale(5)

        val marginFromText = scale(2)

        val x = sectionTextStartPoint.x - marginFromText - hanging
        val y0 = sectionTextStartPoint.y + scale(1)
        val y1 = sectionTextEndPoint.y + editor.lineHeight - scale(2)

        // top margin
        g.drawLine(x, y0, x + scale(2), y0)

        // bottom margin
        g.drawLine(x, y1, x + scale(2), y1)

        // (main) long line
        val g2d = g as Graphics2D
        g2d.stroke = BasicStroke(0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            1.0f, floatArrayOf(3f), 0f)
        g2d.drawLine(x, y0, x, y1)

    }

    private fun Point.newTranslated(dx: Int, dy: Int) = Point(x + dx, y + dy)

    private fun Graphics.fillPolygon(vararg points: Point) =
        fillPolygon(
            points.map { it.x }.toIntArray(),
            points.map { it.y }.toIntArray(),
            points.size
        )
}

