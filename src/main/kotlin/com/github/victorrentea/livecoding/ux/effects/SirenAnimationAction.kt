package com.github.victorrentea.livecoding.ux.effects

import com.github.victorrentea.livecoding.lombok.AddRequiredArgsConstructorInspection
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.LookupOp
import java.awt.image.LookupTable
import javax.swing.Timer
import kotlin.math.floor


class SirenAnimationAction : AbstractAnimationAction() {
    companion object {
        val log = logger<SirenAnimationAction>()
    }
    override fun createAnimationPanel(image: BufferedImage, onFinishedCallback: () -> Unit): AnimationPanel {
        return SirenAnimationPanel(image,onFinishedCallback)
    }

    class SirenAnimationPanel(private val capture: BufferedImage, onFinishedCallback: () -> Unit) : AnimationPanel() {
        private var timer: Timer? = null
        private var convertedImage = capture

        init {
            val cycleTime = 2000
            val startTime = System.currentTimeMillis()

            val refreshDelay = 10
            var lastTime = startTime
            timer = Timer(refreshDelay) { e ->
                val currentTime = System.currentTimeMillis()
                val sinceStart = currentTime - startTime
                val sinceStartOfCycle = sinceStart % cycleTime
                val cycleTimeFraction = 1.0 * sinceStartOfCycle / cycleTime

//                val sinRaw = Math.sin(Math.PI * cycleTimeFraction)
                val sinRaw = if (cycleTimeFraction < .5) cycleTimeFraction * 2 else (1-cycleTimeFraction) * 2

                val sinPercent = floor(sinRaw * 100).toInt()
                val sin = sinPercent / 100.0;

                val frameDelay = currentTime - lastTime
                lastTime = currentTime

                log.debug("$sinceStart => $sin delay=${frameDelay}ms")

                val to = Color.decode("#cc0000")
                val lookup: BufferedImageOp = LookupOp(ToRedColorMapper(to, sinPercent), null)
                convertedImage = lookup.filter(capture, null)

                ApplicationManager.getApplication().invokeLater {
                    repaint()
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
            val t = defaultTransform.createInverse()
            g.drawImage(convertedImage, t, null);
        }
    }

    class ToRedColorMapper(toColor: Color, private val percent: Int) : LookupTable(0, 4) {
        private val to: IntArray

        init {
            this.to = intArrayOf(toColor.red, toColor.green, toColor.blue, toColor.alpha)
        }

        override fun lookupPixel(src: IntArray, dest: IntArray): IntArray {
            System.arraycopy(src, 0, dest, 0, 4)
            for (i in 0..2)
                dest[i] += Math.min(0, (to[i] - dest[i])) * percent / 100
            return dest
        }
    }
}