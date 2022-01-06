package com.github.victorrentea.livecoding.ux

import java.awt.Point
import java.awt.geom.Point2D
import java.awt.image.BufferedImage

class AnimatedGlassPiece(val image: BufferedImage,
                         val relativeCenter:Point2D.Double,
                         var externalCenter:Point2D.Double,
                         val vx: Double, var vy: Double,
                         var rotation: Double,val rotationSpeed: Double) {

}