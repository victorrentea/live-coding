package com.github.victorrentea.livecoding

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.uast.util.classSetOf
import org.jetbrains.uast.util.isInstanceOf

class SyntaxExtractableSectionsVisitor {
    private val sections = mutableSetOf<Pair<Int, Int>>()

    fun getSections() = sections.toList().sortedWith(compareBy({it.first}, {it.second}))

    fun visitElement(element: PsiElement) {
        if (element is PsiCodeBlock) {
            for (i in 0 until element.statements.size) {
                for (j in i until element.statements.size) {
                    sections += element.statements[i].startLineNumber() to element.statements[j].endLineNumber()
                }
            }
        }

        element.children.forEach { visitElement(it) }
    }
}