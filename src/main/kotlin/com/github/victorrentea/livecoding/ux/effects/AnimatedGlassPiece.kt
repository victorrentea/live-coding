package com.github.victorrentea.livecoding.ux.effects

import java.awt.Point
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage

class AnimatedGlassPiece(val image: BufferedImage,
                         val relativeCenter:Point2D.Double,
                         var externalCenter:Point2D.Double,
                         val vx: Double, var vy: Double,
                         var rotation: Double,val rotationSpeed: Double) {
    fun applyTransform(transform: AffineTransform) {
        transform.translate(externalCenter.x, externalCenter.y)
        transform.rotate(rotation, relativeCenter.x, relativeCenter.y)
    }

}