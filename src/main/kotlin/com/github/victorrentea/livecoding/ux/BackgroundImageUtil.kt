package com.github.victorrentea.livecoding.ux

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import javax.swing.Timer
import kotlin.io.path.absolutePathString

object BackgroundImageUtil {
     val log = logger<BackgroundImageUtil>()

    private val CACHED_IMAGE_PATHS = BackgroundImage.values().associateWith { pathFromTemp(it) }
        .also { log.debug("Cached paths from settings: $it") }

    fun clearBackgroundImage() {
        PropertiesComponent.getInstance().setValue(IdeBackgroundUtil.EDITOR_PROP, null)
    }
    fun setBackgroundImage(image: BackgroundImage, opacity: Int) {
        fadeoutTimer?.stop()
        justSetBackgroundImage(image, opacity)
    }

    private fun justSetBackgroundImage(image: BackgroundImage, opacity: Int) {
        val filePath = CACHED_IMAGE_PATHS[image]
        val imgPath = filePath + "," + opacity
        log.debug("Path $imgPath")
        PropertiesComponent.getInstance().setValue(IdeBackgroundUtil.EDITOR_PROP, imgPath)
    }

    private var fadeoutTimer: Timer? = null

    fun setBackgroundImageFadingOut(image: BackgroundImage, initialOpacity: Int, fadeoutSeconds: Int) {
        fadeoutTimer?.stop()
        justSetBackgroundImage(image, initialOpacity)
        val t0 = System.currentTimeMillis()
        val endTime = t0 + fadeoutSeconds * 1000
        fadeoutTimer = Timer(2000) {
            val t = System.currentTimeMillis()
            if (t > endTime) {
                justSetBackgroundImage(image, 0)
                fadeoutTimer?.stop()
            } else {
                val currentOpacity = initialOpacity * (endTime - t) / (endTime - t0)
                println("opacity: $currentOpacity")
                justSetBackgroundImage(image, currentOpacity.toInt())
            }
        }
        fadeoutTimer?.start()
        println("Started")
    }

    private fun pathFromTemp(image: BackgroundImage): String {
        val savedPaths = AppSettingsState.getInstance().unzippedImagedPaths

        savedPaths.entries.removeIf { (_, path) -> !File(path).isFile } // temp file was deleted

        return savedPaths.computeIfAbsent(image) { copyImageToTemp(image) }
    }

    private fun copyImageToTemp(image: BackgroundImage): String {
        val tempFilePath = Files.createTempFile(image.fileName, ".jpg").absolutePathString()
        image.url().openStream().copyTo(FileOutputStream(tempFilePath))
        log.debug("Successfully copied background image in temp folder at $tempFilePath")
        return tempFilePath
    }
}

enum class BackgroundImage(val fileName: String) {
    HORROR("horror"),
    CHILD("child"),
    COOL("cool"),
    RED("red"),
    GREEN("green");

    fun url() = BackgroundImageUtil::class.java.classLoader.getResource(pathInClassPath())
        ?: throw IllegalArgumentException("Image not found in classpath of plugin jar: " + pathInClassPath())
    private fun pathInClassPath() = "icons/$fileName.jpg"
}