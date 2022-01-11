package com.github.victorrentea.livecoding.ux.effects

import java.awt.image.BufferedImage

class ShakeAnimationAction : AbstractAnimationAction() {
    override fun createAnimationPanel(image: BufferedImage, onFinishedCallback: () -> Unit): AnimationPanel {
        return ShakeAnimationPanel(image,onFinishedCallback)
    }
}