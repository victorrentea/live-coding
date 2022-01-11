package com.github.victorrentea.livecoding.ux.effects

import java.awt.image.BufferedImage

class BrokenGlassAnimationAction : AbstractAnimationAction() {
    override fun createAnimationPanel(image: BufferedImage, onEndCallback: () -> Unit): AnimationPanel {
        return BrokenGlassAnimationPanel(image,onEndCallback)
    }
}