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

        val marginFromText = scale(4)

        val x = sectionTextStartPoint.x - marginFromText - hanging
        val y0 = sectionTextStartPoint.y + scale(4)
        val y1 = sectionTextEndPoint.y + editor.lineHeight - scale(4)

        // top margin
//        g.drawLine(x, y0, x + scale(2), y0)

        // bottom margin
//        g.drawLine(x, y1, x + scale(2), y1)

        // (main) long line
        val g2d = g as Graphics2D
        val newGraphics = g2d.create() as Graphics2D
        try {
            newGraphics.color = Color(195,214,232)
            newGraphics.stroke = BasicStroke(
                3.toFloat()
                //, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL
//                ,1.0f, floatArrayOf(3f), 0f
            )
            newGraphics.drawLine(x, y0, x, y1)
        } finally { newGraphics.dispose() }

    }

    private fun Point.newTranslated(dx: Int, dy: Int) = Point(x + dx, y + dy)

    private fun Graphics.fillPolygon(vararg points: Point) =
        fillPolygon(
            points.map { it.x }.toIntArray(),
            points.map { it.y }.toIntArray(),
            points.size
        )
}

