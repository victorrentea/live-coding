package com.github.victorrentea.livecoding.ux

import com.github.victorrentea.livecoding.ux.CaptureBreakGlassAnimationPanel.GlassPoint.*
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import java.awt.*
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.Closeable
import javax.swing.JPanel
import javax.swing.Timer

class CaptureBreakGlassAnimationPanel(private val fullImage: Image) : AnimationPanel() {
    enum class GlassPoint(val dx: Double, val dy: Double) {
        LEFT_TOP(0.0, 0.0),
        RIGHT_TOP(1.0, 0.0),
        RIGHT_BOTTOM(1.0, 1.0),
        LEFT_BOTTOM(0.0, 1.0),

        A(0.25, 0.0),
        B(0.45, 0.0),
        C(0.47, 0.5),
        D(0.0, 0.53),
        E(0.4, 0.51),
        F(0.35, 1.0),
        G(0.51, 1.0),
        H(0.62, 0.6),
        I(0.75, 1.0),
        J(1.0, 0.95)
    }

    companion object {
        val piecesPoints = listOf(
            listOf(LEFT_TOP, A, C, E, D),
            listOf(A, C, B),
            listOf(B, C, H, J, RIGHT_TOP),
            listOf(I, H, J, RIGHT_BOTTOM),
            listOf(G, C, H, I),
            listOf(F, E, C, G),
            listOf(D, E, F, LEFT_BOTTOM)
        )

    }

    override fun close() {
        timer?.stop();
    }

    var timer: Timer? = null
    val allPieces = computePiecesClip().map { extractOnePiece(it) }

    init {
        var iteration = 0
        val nIterations = 2000
        timer = Timer(20) {
            ApplicationManager.getApplication().invokeLater {
                for (piece in allPieces) {
                    piece.rotation += piece.rotationSpeed
                    piece.vy += .5 // gravity
                    piece.externalCenter.x += piece.vx
                    piece.externalCenter.y += piece.vy
                    println("Piece " + piece.externalCenter.y)
                }
                repaint()
            }

            iteration++
            if (iteration == nIterations) {
                timer?.stop()
                timer = null
                println("FINISHED ANIMATION")
            }
        }
        timer?.start()
    }

    fun computePiecesClip(): List<Polygon> {
        val width = fullImage.getWidth(null)
        val height = fullImage.getHeight(null)
        return piecesPoints.map { glassPoints ->
            val xList = glassPoints.map { (it.dx * width).toInt() }.toIntArray()
            val yList = glassPoints.map { (it.dy * height).toInt() }.toIntArray()
            Polygon(xList, yList, glassPoints.size)
        }
    }


    override fun paint(g: Graphics?) {
        val g2d = g as? Graphics2D ?: return
        val defaultTransform = g2d.deviceConfiguration.defaultTransform ?: return
        val t = defaultTransform.createInverse();

        println("print clipped")
        g.color = Color.black
        g.fillRect(0, 0, width, height)

        allPieces.forEachIndexed { index, image ->
            val myT = t.deepClonePolymorphic()
            myT.translate(image.externalCenter.x,image.externalCenter.y)
            myT.rotate(image.rotation, image.relativeCenter.x, image.relativeCenter.y)
            g.drawImage(image.image, myT, null)
        }

    }

    private fun extractOnePiece(polygon: Polygon): AnimatedGlassPiece {
        val width = fullImage.getWidth(null)
        val height = fullImage.getHeight(null)
        val imagePiece = BufferedImage(
            width, height,
            BufferedImage.TYPE_INT_ARGB
        )
        val g = imagePiece.graphics
        g.clip = polygon
        g.drawImage(fullImage, 0, 0, null)
        g.dispose()

        val relativeCenter = polygon.bounds2D.let { Point2D.Double(it.centerX, it.centerY) }
        return AnimatedGlassPiece(
            imagePiece, relativeCenter, Point2D.Double(0.0,0.0),
            -0.1 * Math.random(),
            -0.6 * Math.random(),
            0.0, (Math.random() - 0.5) * 0.01
        )
//        return imagePiece
    }
}