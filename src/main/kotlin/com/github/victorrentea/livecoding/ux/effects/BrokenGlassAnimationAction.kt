package com.github.victorrentea.livecoding.ux.effects

import java.awt.image.BufferedImage

class BrokenGlassAnimationAction : CaptureAnimationAction() {
    override fun createAnimationPanel(image: BufferedImage): AnimationPanel {
        return BrokenGlassAnimationPanel(image)
    }
}