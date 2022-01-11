package com.github.victorrentea.livecoding.ux.effects

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import javax.swing.JFrame


abstract class AbstractAnimationAction : AnAction() {
    companion object {
        private val log = logger<AbstractAnimationAction>()
    }
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.HOST_EDITOR)
        val gc = editor?.component?.graphicsConfiguration ?: return


//        gc.device.

        val screenRect = Rectangle(Toolkit.getDefaultToolkit().getScreenSize())
//        log.debug("Capturing $screenRect")

        val mrImage: MultiResolutionImage = Robot().createMultiResolutionScreenCapture(screenRect)
        val resolutionVariants = mrImage.resolutionVariants
        val nativeResImage: Image = if (resolutionVariants.size > 1) {
            resolutionVariants[1]
        } else {
            resolutionVariants[0]
        }
        val capturedXPx = nativeResImage.getWidth(null)
        val capturedYPx = nativeResImage.getHeight(null)
//        log.debug("Screen capture width = " + capturedXPx)

        val scaledBufferedImg = BufferedImage(
            capturedXPx, capturedYPx,
            BufferedImage.TYPE_INT_ARGB
        )
        val g = scaledBufferedImg.graphics
        g.drawImage(nativeResImage, 0, 0, null)
        g.dispose()

         // TODO make sure this frame is opened on the same monitor as the Editor
        val frame = JFrame("Overlay", gc)

        gc.device.displayMode.let {
            log.debug("Screen size: $it")
            frame.setSize(it.width, it.height)
        }
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.extendedState = Frame.MAXIMIZED_BOTH
        frame.isUndecorated = true
        frame.isVisible = true
        frame.isFocusable = true
        frame.contentPane.layout = BorderLayout()
        val panel = createAnimationPanel(scaledBufferedImg) {
            frame.isVisible = false
            frame.dispose()
        }

        fun stopAnimation() {
            frame.isVisible = false
            panel.close()
            frame.dispose()
        }

        frame.contentPane.add(panel, BorderLayout.CENTER)
        frame.addWindowFocusListener(object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent?) {}
            override fun windowLostFocus(e: WindowEvent?) {
                stopAnimation()
            }
        })
        frame.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
                if (e?.isAltDown == true) return // ignore alt tab
                stopAnimation()
            }
            override fun keyPressed(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent?) {}
        })

    }
   abstract fun createAnimationPanel(image: BufferedImage, onEndCallback: () -> Unit): AnimationPanel
}

