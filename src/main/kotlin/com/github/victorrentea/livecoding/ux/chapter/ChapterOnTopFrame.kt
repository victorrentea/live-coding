package com.github.victorrentea.livecoding.ux.chapter

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel

class ChapterOnTopFrame(chapter: Chapter) : JFrame() {
    init {
        isAlwaysOnTop = true
        isUndecorated = true
        background = Color(0, 0, 0, 0)
        focusableWindowState = false
        isFocusable = false
        contentPane = TranslucentPane()

        val b = JButton(chapter.title)
        var tEnter: Long = 0
        var yLocationTop = true
        b.addMouseListener(object: MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                tEnter = System.currentTimeMillis()
            }

            override fun mouseExited(e: MouseEvent?) {
                val timeHovered = System.currentTimeMillis() - tEnter
                if (timeHovered > 1000) {
                    yLocationTop = !yLocationTop
                    if (yLocationTop) {
                        location = Point(location.x, 0)
                    } else {
                        location = Point(location.x, Toolkit.getDefaultToolkit().screenSize.height-size.height)
                    }
                    repaint()
                }
            }
        })
        b.background = Color.yellow
        add(b)
        size = Dimension(b.size)

        val x = (Toolkit.getDefaultToolkit().screenSize.width - size.width) / 2
        location = Point(x, 0)
        pack()
        isVisible = true
    }

    class TranslucentPane : JPanel() {
        init {
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g.create() as Graphics2D
            g2d.composite = AlphaComposite.SrcOver.derive(0f)
            g2d.color = background
            g2d.fillRect(0, 0, width, height)
        }
    }
}