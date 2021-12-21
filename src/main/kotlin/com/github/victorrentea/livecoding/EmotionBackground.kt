package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.absolutePathString


class EmotionBackground : AnAction() {
    companion object {
        private var on = false
        private val autoHorrorBgPath = spawnBackgroundImage()
        fun spawnBackgroundImage(): String? {
            if (AppSettingsState.getInstance().unzippedHorrorImagePath?.let{ File(it)}?.isFile == true) {
                return AppSettingsState.getInstance().unzippedHorrorImagePath
            }
            val tempFilePath = Files.createTempFile("horror-bg", ".jpg").absolutePathString()
            val internalUrl = EmotionBackground::class.java.classLoader.getResource("icons/horror.jpg")
            if (internalUrl == null) {
                println("Image not found in plugin jar!")
                return null
            }
            internalUrl.openStream().copyTo(FileOutputStream(tempFilePath))
            println("Successfully stored background image at $tempFilePath")
            AppSettingsState.getInstance().unzippedHorrorImagePath = tempFilePath
            return tempFilePath
        }
        init {
            ApplicationManager.getApplication().invokeLater {
                val prop = PropertiesComponent.getInstance()
                prop.setValue(IdeBackgroundUtil.EDITOR_PROP, null)
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        ApplicationManager.getApplication().invokeLater {
            on = !on;
            val prop = PropertiesComponent.getInstance()
            prop.setValue(IdeBackgroundUtil.FRAME_PROP, null);

            val bgImagePath = /*AppSettingsState.getInstance().hardCoreImageBackgroundPath?.let {  } ?:*/ autoHorrorBgPath;
            val bgWithOpacity = bgImagePath?.let { "$it,50" }
            println("BG: $bgWithOpacity")

            prop.setValue(IdeBackgroundUtil.EDITOR_PROP, if (on) bgWithOpacity else null)
            IdeBackgroundUtil.repaintAllWindows()
            if (on) {
                editor?.let { editor ->
                    HintManager.getInstance().showErrorHint(editor, "Entering hard-core mode ...")
                }
            }
        }
    }
}