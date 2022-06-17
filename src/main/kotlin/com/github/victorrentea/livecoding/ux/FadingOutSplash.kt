package com.github.victorrentea.livecoding.ux

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer


class FadingOutSplash(fileName: String) {
    companion object {
        val log = logger<FadingOutSplash>()
    }

    init {
        val fadeoutSeconds = 5
        val initialOpacity = 0.7f

        val frame = JFrame()
        val imagePanel = ImagePanel(initialOpacity, fileName)
        frame.contentPane.add(imagePanel)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isUndecorated = true
        frame.pack()

        val gd = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        val width = gd.displayMode.width
        val height = gd.displayMode.height
        frame.setLocation(width - 500, height - 500)
//        frame.setLocationRelativeTo(null)
        frame.isAlwaysOnTop=true
        frame.focusableWindowState = false
        frame.background = Color(0, 0, 0, 0)
        frame.isVisible = true
        frame.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                frame.dispose()
            }
        })

        val t0 = System.currentTimeMillis()
        val endTime = t0 + fadeoutSeconds * 1000
        val fadeoutTimer = Timer(500) {
            val t = System.currentTimeMillis()
            if (t > endTime) {
                val timer = it.source as Timer
                timer.stop()
                frame.dispose();
            } else {
                val currentOpacity = initialOpacity * (endTime - t) / (endTime - t0)
                println("opacity: $currentOpacity")
                imagePanel.opacity = currentOpacity;
                invokeLater { frame.repaint() }
            }
        }
        fadeoutTimer.start()
        println("Started")
    }

    inner class ImagePanel( var opacity: Float, fileName: String) : JPanel() {
        var img: BufferedImage? = null
        init {
            isOpaque = false
            layout = GridBagLayout()
            FadingOutSplash.javaClass.getResourceAsStream("/icons/${fileName}.png")
                .use {
                    try {
                        img = ImageIO.read(it)
                    } catch (ex: IOException) {
                        log.error(ex);
                    }
                }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g.create() as Graphics2D
//            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
            g2d.composite = AlphaComposite.SrcOver.derive(opacity)
            g2d.drawImage(img, 0, 0, width, height, this)
            g2d.dispose()
        }

        override fun getPreferredSize(): Dimension {
            return Dimension(500, 500)
        }
    }
}