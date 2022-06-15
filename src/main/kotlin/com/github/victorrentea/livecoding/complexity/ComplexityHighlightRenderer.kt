//package com.github.victorrentea.livecoding.complexity
//
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
//import com.intellij.openapi.editor.markup.RangeHighlighter
//import com.intellij.ui.scale.JBUIScale.scale
//import java.awt.Color
//import java.awt.FontMetrics
//import java.awt.Graphics
//
//
//class ComplexityHighlightRenderer(private val complexity: Int) : CustomHighlighterRenderer {
//    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
//        g.color = when (complexity) {
////            in 2..4 -> Color.green
//            in 5..9 -> Color(255,153,51)
//            in 10..1000 -> Color.red
//            else -> return
//        }
//        val methodSignatureEndPoint =
//            editor.logicalPositionToXY(editor.offsetToLogicalPosition(highlighter.endOffset))
//
//        val RIGHT_MARGIN_FROM_TEXT = scale(8)
//
//        val s = " $complexity = Cognitive Complexity"
//
//        val x = methodSignatureEndPoint.x + RIGHT_MARGIN_FROM_TEXT
//        val y = methodSignatureEndPoint.y + editor.lineHeight / 2
//        val fm: FontMetrics = g.getFontMetrics()
//        val swidth = fm.stringWidth(s)
//
//        val mheight = fm.ascent - fm.descent - fm.leading
//
//        g.drawString(s, x, y + mheight / 2)
//    }
//}
