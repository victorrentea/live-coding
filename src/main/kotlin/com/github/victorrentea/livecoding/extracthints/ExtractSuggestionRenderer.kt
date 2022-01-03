package com.github.victorrentea.livecoding.extracthints

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.scale.JBUIScale.scale
import java.awt.Color
import java.awt.Graphics
import java.awt.Point

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

        g.drawLine(sectionTextStartPoint.x - marginFromText - hanging,
            sectionTextStartPoint.y + scale(1),
            sectionTextStartPoint.x - marginFromText - hanging,
            sectionTextEndPoint.y + editor.lineHeight - scale(2))

        g.drawLine(sectionTextStartPoint.x - marginFromText - hanging,
            sectionTextStartPoint.y + scale(1),
            sectionTextStartPoint.x - marginFromText - hanging + scale(2),
            sectionTextStartPoint.y + scale(1))

        g.drawLine(sectionTextStartPoint.x - marginFromText - hanging,
            sectionTextEndPoint.y + editor.lineHeight - scale(2),
            sectionTextStartPoint.x - marginFromText - hanging + scale(2),
            sectionTextEndPoint.y + editor.lineHeight - scale(2))

        // line
//        g.fillRect(
//            sectionTextStartPoint.x - LEFT_MARGIN_FROM_TEXT - (2) - hanging,
//            sectionTextStartPoint.y + (1),
//            (2),
//            sectionTextEndPoint.y - sectionTextStartPoint.y + editor.lineHeight - (2)
//        )
        // start triangle
//        val topTrianglePoint0 = Point(
//            sectionTextStartPoint.x - LEFT_MARGIN_FROM_TEXT - hanging,
//            sectionTextStartPoint.y + (1)
//        )
//        g.fillPolygon(
//            topTrianglePoint0,
//            topTrianglePoint0.newTranslated((4), 0),
//            topTrianglePoint0.newTranslated(0, (4))
//        )
//
//        // end triangle
//        val topTrianglePoint1 = Point(
//            sectionTextStartPoint.x - LEFT_MARGIN_FROM_TEXT - hanging,
//            sectionTextEndPoint.y+ editor.lineHeight - (1)
//        )
//        g.fillPolygon(
//            topTrianglePoint1,
//            topTrianglePoint1.newTranslated((4), 0),
//            topTrianglePoint1.newTranslated(0, (-4))
//        )
    }
    private fun Point.newTranslated(dx: Int, dy: Int) = Point(x + dx, y + dy)

    private fun Graphics.fillPolygon(vararg points:Point) =
        fillPolygon(
            points.map { it.x }.toIntArray(),
            points.map { it.y }.toIntArray(),
            points.size
        )
}

