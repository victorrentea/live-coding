package com.github.victorrentea.livecoding.ux.effects

import java.awt.image.BufferedImage

class ShakeAnimationAction : CaptureAnimationAction() {
    override fun createAnimationPanel(image: BufferedImage): AnimationPanel {
        return ShakeAnimationPanel(image)
    }
}