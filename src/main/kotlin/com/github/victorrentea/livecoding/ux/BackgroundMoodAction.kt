package com.github.victorrentea.livecoding.ux

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.impl.IdeBackgroundUtil

enum class BackgroundMood(val label: String, val image: BackgroundImage, val opacity: Int ) {
    MOOD1("Hard-core Mode!", BackgroundImage.HORROR, 50),
    MOOD2("Let's Relax...", BackgroundImage.CHILD, 15),
    MOOD3("Geek Time 🤓", BackgroundImage.COOL, 30)
}

private var state: BackgroundMood? = null

open class BackgroundMoodAction(private val mood: BackgroundMood?) : AnAction() {
    companion object {
        protected val log = logger<BackgroundMoodAction>()
        init {
            ApplicationManager.getApplication().invokeLater {
                val prop = PropertiesComponent.getInstance()
                prop.setValue(IdeBackgroundUtil.EDITOR_PROP, null)
                log.debug("Cleaned up the background at startup")
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        ApplicationManager.getApplication().invokeLater {
            state = if (state == mood) null else mood
            val currentState = state;
            if (currentState != null) {
                BackgroundImageUtil.setBackgroundImage(currentState.image, currentState.opacity)
                editor?.let { HintManager.getInstance().showErrorHint(it, currentState.label) }
            } else {
                BackgroundImageUtil.clearBackgroundImage()
            }
        }
    }
}

class BackgroundMood1Action : BackgroundMoodAction(BackgroundMood.MOOD1)

class BackgroundMood2Action : BackgroundMoodAction(BackgroundMood.MOOD2)

class BackgroundMood3Action : BackgroundMoodAction(BackgroundMood.MOOD3)

class BackgroundMoodResetAction : BackgroundMoodAction(null)



