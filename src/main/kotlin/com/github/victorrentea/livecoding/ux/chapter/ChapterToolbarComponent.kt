package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JPanel

class ChapterToolbarComponent(setTitleAction: (Project) -> Unit) : JPanel() {
    companion object {
        private const val EMPTY_TITLE = "Chapter..."
    }
    private val titleButton = JButton().also {
        it.isFocusable = false
        it.toolTipText = "Start new chapter"
        it.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                DataManager.getInstance().getDataContext(e.component).getData(CommonDataKeys.PROJECT)?.let {setTitleAction(it) }
            }
        })
    }
    init {
        add(titleButton)
    }
    fun setChapter(chapter : Chapter?) {
        if (chapter != null) {
            titleButton.text = chapter.title + " (since " + chapter.startTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ")"
            titleButton.background = Color.yellow
        } else {
            titleButton.text = EMPTY_TITLE
            titleButton.background = null
        }
    }
}