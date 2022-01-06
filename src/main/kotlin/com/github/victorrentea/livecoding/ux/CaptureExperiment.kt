package com.github.victorrentea.livecoding.ux

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer


class CaptureExperimentAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.HOST_EDITOR)
        val gc = editor?.component?.graphicsConfiguration!!

        val screenRect = Rectangle(Toolkit.getDefaultToolkit().getScreenSize())
//        println("Capturing $screenRect")

        val mrImage: MultiResolutionImage = Robot().createMultiResolutionScreenCapture(screenRect)
        val resolutionVariants = mrImage.resolutionVariants
        val nativeResImage: Image = if (resolutionVariants.size > 1) {
            resolutionVariants[1]
        } else {
            resolutionVariants[0]
        }
        val capturedXPx = nativeResImage.getWidth(null)
        val capturedYPx = nativeResImage.getHeight(null)
//        println("Screen capture width = " + capturedXPx)

        val scaledBufferedImg = BufferedImage(
            capturedXPx, capturedYPx,
            BufferedImage.TYPE_INT_ARGB
        )
        val g = scaledBufferedImg.graphics
        g.drawImage(nativeResImage, 0, 0, null)
        g.dispose()

//        println("displaymode w: " + gc.device.displayMode.width)

        val frame = JFrame("Overlay", gc)

        gc.device.displayMode.let {
            println("Screen size: $it")
            frame.setSize(it.width, it.height)
        }
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.extendedState = Frame.MAXIMIZED_BOTH
        frame.isUndecorated = true
        frame.isVisible = true
        frame.isFocusable = true
        frame.contentPane.layout = BorderLayout()
        val panel = MyPanel(scaledBufferedImg)
        frame.contentPane.add(panel, BorderLayout.CENTER)
        frame.addWindowFocusListener(object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent?) {
            }
            override fun windowLostFocus(e: WindowEvent?) {
                frame.isVisible = false
            }
        })
        frame.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
            }
            override fun keyPressed(e: KeyEvent?) {
            }
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ESCAPE) {
                    frame.isVisible = false
                }
            }
        })
    }
}

class MyPanel(private val capture: Image) : JPanel() {
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

