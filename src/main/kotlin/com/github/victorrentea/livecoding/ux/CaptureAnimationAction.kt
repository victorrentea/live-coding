package com.github.victorrentea.livecoding.ux

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import javax.swing.JFrame


class CaptureAnimationAction : AnAction() {
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
        val panel = CaptureAnimationPanel(scaledBufferedImg)
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

