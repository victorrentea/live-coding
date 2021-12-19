package com.github.victorrentea.livecoding

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import java.io.File

const val JAVA_SRC_FOLDER = "src/test/live-coding-playground/src/main/java"
val javaSrcFolder = File(JAVA_SRC_FOLDER)

fun getInputFilePaths(folderName: String): List<String> {
    val folder = File(javaSrcFolder, folderName)
    if (!folder.exists()) throw IllegalArgumentException("Folder does not exist: ${folder.absolutePath}")
    val files = folder.walkTopDown().filter { it.isFile }.toList()
    if (files.isEmpty()) throw IllegalArgumentException("No files found in ${folder.absolutePath}")
    val paths = files.map { it.toRelativeString(javaSrcFolder) }
    println("FOUND FILE NAMES: $paths")
    return paths
}

fun getExpectedFile(inputFilePath: String) = "../../test/java/$inputFilePath"


