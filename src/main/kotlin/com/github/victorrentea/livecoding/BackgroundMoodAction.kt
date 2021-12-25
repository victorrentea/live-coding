package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.absolutePathString

enum class BackgroundMood {
    MOOD1,
    MOOD2,
    MOOD3,
    NONE
}

private val log = logger<BackgroundMoodAction>()
private var state = BackgroundMood.NONE

private val DEFAULT_IMAGE_PATHS = mapOf(
    BackgroundMood.MOOD1 to pathFromTemp("horror", BackgroundMood.MOOD1) + ",50",
    BackgroundMood.MOOD2 to pathFromTemp("child", BackgroundMood.MOOD2) + ",20",
    BackgroundMood.MOOD3 to pathFromTemp("cool", BackgroundMood.MOOD3) + ",30"
)

private fun pathFromTemp(imgNameInResources: String, mood: BackgroundMood): String? {
    val savedPaths = AppSettingsState.getInstance().unzippedImagedPaths
    log.debug("unzippedimages: " + savedPaths)

    savedPaths.entries.removeIf { (_, path) -> !File(path!!).isFile } // temp file was deleted

    return savedPaths.computeIfAbsent(mood) { copyToTemp(imgNameInResources) }
}

private fun copyToTemp(imgNameInResources: String): String {
    val internalUrl = BackgroundMoodAction::class.java.classLoader.getResource("icons/$imgNameInResources.jpg")
        ?: throw IllegalArgumentException("Image not found in plugin jar!")
    val tempFilePath = Files.createTempFile("$imgNameInResources-background", ".jpg").absolutePathString()
    internalUrl.openStream().copyTo(FileOutputStream(tempFilePath))
    log.debug("Successfully copied background image in temp folder at $tempFilePath")
    return tempFilePath
}

open class BackgroundMoodAction(val moodName: String?, val mood: BackgroundMood) : AnAction() {
    companion object {
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
            val prop = PropertiesComponent.getInstance()
            prop.setValue(IdeBackgroundUtil.EDITOR_PROP, null)

            state = if (state == mood) BackgroundMood.NONE else mood

            val imgPath = DEFAULT_IMAGE_PATHS[state]
            prop.setValue(IdeBackgroundUtil.EDITOR_PROP, imgPath)

            log.debug("Path " + imgPath)

            if (state != BackgroundMood.NONE && moodName != null) {
                editor?.let { HintManager.getInstance().showErrorHint(it, "Entering $moodName Mode ...") }
            }
            IdeBackgroundUtil.repaintAllWindows()
        }
    }
}

class BackgroundMood1Action : BackgroundMoodAction("Hard-core", BackgroundMood.MOOD1) {
}

class BackgroundMood2Action : BackgroundMoodAction("Relax", BackgroundMood.MOOD2) {
}

class BackgroundMood3Action : BackgroundMoodAction("Geek", BackgroundMood.MOOD3) {
}

class BackgroundMoodResetAction : BackgroundMoodAction(null, BackgroundMood.NONE)