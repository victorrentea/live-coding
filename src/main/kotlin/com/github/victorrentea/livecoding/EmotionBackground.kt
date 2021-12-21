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

enum class BackgroundType {
    MOOD1,
    MOOD2,
    MOOD3,
    NONE
}

class BackgroundMoodUtil {
    companion object {
        var state = BackgroundType.NONE

        val imgPaths = mapOf<BackgroundType, String>(
            BackgroundType.MOOD1 to spawnBackgroundImage("horror", BackgroundType.MOOD1) + ",50",
            BackgroundType.MOOD2 to spawnBackgroundImage("child", BackgroundType.MOOD2) + ",20",
            BackgroundType.MOOD3 to spawnBackgroundImage("cool", BackgroundType.MOOD3) + ",30"
        )

        fun spawnBackgroundImage(imgName: String, mood: BackgroundType): String {
            val savedPaths = AppSettingsState.getInstance().unzippedImagedPaths

            savedPaths.entries.removeIf { (_, path) -> !File(path!!).isFile }
            return savedPaths.computeIfAbsent(mood) {

                val tempFilePath = Files.createTempFile("$imgName-background", ".jpg").absolutePathString()
                val internalUrl = BackgroundMoodUtil::class.java.classLoader.getResource("icons/$imgName.jpg")
                    ?: throw IllegalArgumentException("Image not found in plugin jar!")
                internalUrl.openStream().copyTo(FileOutputStream(tempFilePath))
                println("Successfully stored background image at $tempFilePath")
                tempFilePath
            }!!
        }

        init {
            ApplicationManager.getApplication().invokeLater {
                val prop = PropertiesComponent.getInstance()
                prop.setValue(IdeBackgroundUtil.EDITOR_PROP, null)
            }
        }
    }
}

open class BackgroundMoodBaseAction(val name: String, val mood: BackgroundType) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ApplicationManager.getApplication().invokeLater {
            val prop = PropertiesComponent.getInstance()
            prop.setValue(IdeBackgroundUtil.EDITOR_PROP, null)
            if (BackgroundMoodUtil.state == mood) {
                BackgroundMoodUtil.state = BackgroundType.NONE
            } else {
                BackgroundMoodUtil.state = mood
                val imgPath = BackgroundMoodUtil.imgPaths[mood]

                prop.setValue(IdeBackgroundUtil.EDITOR_PROP, imgPath)
                val editor = e.getData(CommonDataKeys.EDITOR)
                editor?.let { HintManager.getInstance().showErrorHint(it, "Entering $name Mode ...") }
            }
        }
        IdeBackgroundUtil.repaintAllWindows()
    }
}

class BackgroundMood1Action : BackgroundMoodBaseAction("Hard-core", BackgroundType.MOOD1) {
}

class BackgroundMood2Action : BackgroundMoodBaseAction("Relax", BackgroundType.MOOD2) {
}

class BackgroundMood3Action : BackgroundMoodBaseAction("Geek", BackgroundType.MOOD3) {
}