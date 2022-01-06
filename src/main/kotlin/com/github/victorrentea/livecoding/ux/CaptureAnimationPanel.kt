package com.github.victorrentea.livecoding.ux

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JPanel
import javax.swing.Timer

class CaptureAnimationPanel(private val capture: Image) : JPanel() {
    init {
        addMouseListener(object : MouseListener {
            override fun mouseClicked(mouseEvent: MouseEvent?) {
                val duration = 1500
                val refreshMillis = 20
                var iteration = 0
                val nIterations = duration/refreshMillis
                val nShakes = 30
                val shakeMaxAmpl = 30.0 // px
                if (timer != null) return
                timer = Timer(refreshMillis) { e ->
                    val ampl = shakeMaxAmpl * (nIterations - iteration) / nIterations
                    val sin = Math.sin(6.0 * nShakes * iteration / nIterations)
                    dx = (ampl * sin).toInt()

                    iteration++
//                    println("Anim $iteration dx=$dx")
                    repaint()

                    if (iteration == nIterations) {
                        timer?.stop()
                        timer = null
                    }
                }
                timer?.start()
            }

            override fun mousePressed(e: MouseEvent?) {
            }

            override fun mouseReleased(e: MouseEvent?) {
            }

            override fun mouseEntered(e: MouseEvent?) {
            }

            override fun mouseExited(e: MouseEvent?) {
            }

        })
    }

    var dx: Int = 0
    var dy: Int = 0
    var timer: Timer? = null
    override fun paint(g: Graphics?) {
        val g2d = g as? Graphics2D ?: return
        val defaultTransform = g2d.deviceConfiguration.defaultTransform ?: return
        val t = defaultTransform.createInverse();
        t.translate(dx.toDouble(), -0.3*dx)
        g.color = Color.black
        g.drawRect(0, 0, width, height)
        g.drawImage(capture, t, null);
    }
}