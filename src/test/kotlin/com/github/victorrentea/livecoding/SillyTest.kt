package com.github.victorrentea.livecoding

import org.junit.Test
import java.io.File

class SillyTest {

    @Test
    fun testX() {
        val folder = File(MyPluginParameterizedTest.JAVA_SRC_FOLDER + "/splitvariable")
        if (!folder.exists()) throw IllegalArgumentException("Folder does not exist: ${folder.absolutePath}")
        val files = folder.walkTopDown().toList()
        if (files.isEmpty()) throw IllegalArgumentException("No files found in ${folder.absolutePath}")
        val names = files.map { it.absolutePath.substringAfter(MyPluginParameterizedTest.JAVA_SRC_FOLDER + "/") }
        println("FOUND FILES: $files")
    }
}