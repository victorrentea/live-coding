package com.github.victorrentea.livecoding.extracthints

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.scale.JBUIScale.scale
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Point

class ComplexMethodRenderer(private val complexity: Int) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        g.color = Color.blue
        val methodSignatureEndPoint =
            editor.logicalPositionToXY(editor.offsetToLogicalPosition(highlighter.endOffset))

        val RIGHT_MARGIN_FROM_TEXT = scale(10)

//        g.font = Font.
        g.drawLine(methodSignatureEndPoint.x + RIGHT_MARGIN_FROM_TEXT, methodSignatureEndPoint.y,
            methodSignatureEndPoint.x + RIGHT_MARGIN_FROM_TEXT + 10, methodSignatureEndPoint.y)
        g.drawString("Complex🧠: " + complexity, methodSignatureEndPoint.x + RIGHT_MARGIN_FROM_TEXT, methodSignatureEndPoint.y)
    }
}
