package com.github.victorrentea.livecoding.ux

import com.intellij.openapi.application.ApplicationManager
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JPanel
import javax.swing.Timer

class CaptureShakeAnimationPanel(private val capture: Image) : AnimationPanel() {
    var timer: Timer? = null
    var dx: Int = 0

    init {
        val duration = 1500
        val refreshMillis = 20
        var iteration = 0
        val nIterations = duration/refreshMillis
        val nShakes = 30
        val shakeMaxAmpl = 30.0 // px
        timer = Timer(refreshMillis) { e ->
            ApplicationManager.getApplication().invokeLater {

                val ampl = shakeMaxAmpl * (nIterations - iteration) / nIterations
                val sin = Math.sin(6.0 * nShakes * iteration / nIterations)
                dx = (ampl * sin).toInt()
                repaint()
            }

            iteration++
            if (iteration == nIterations) {
                timer?.stop()
                timer = null
            }
        }
        timer?.start()
    }

    override fun close() {
        timer?.stop();
    }

    override fun paint(g: Graphics?) {
        val g2d = g as? Graphics2D ?: return
        val defaultTransform = g2d.deviceConfiguration.defaultTransform ?: return
        val t = defaultTransform.createInverse();
        t.translate(dx.toDouble(), -0.3*dx)
        g.color = Color.black
        g.fillRect(0, 0, width, height)
        g.drawImage(capture, t, null);
    }
}