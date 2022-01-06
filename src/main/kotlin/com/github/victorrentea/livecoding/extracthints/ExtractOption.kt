package com.github.victorrentea.livecoding.extracthints

import com.github.victorrentea.livecoding.complexity.CognitiveComplexityInContext
import com.intellij.psi.PsiStatement

data class ExtractOption(
    val lines: Pair<Int, Int>,
    val section: List<PsiStatement>,
    val parameterCount: Int,
    val complexity: CognitiveComplexityInContext,
    val depth: Int,
    var displayHanging: Int = 0
) {
    fun containedWithin(other: ExtractOption): Boolean =
        startLine >= other.startLine && endLine <= other.endLine

    fun intersects(other: ExtractOption): Boolean =
        startLine <= other.endLine && other.startLine <= endLine

    val lineCount get() = endLine - startLine + 1
    val startLine get() = lines.first
    val endLine get() = lines.second
}