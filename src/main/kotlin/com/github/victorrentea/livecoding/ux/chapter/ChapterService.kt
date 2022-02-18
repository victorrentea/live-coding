package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern

@State(name = "ChapterService",  storages = [Storage("chapter4.xml")])
class ChapterService : PersistentStateComponent<ChapterService.ChapterState> {
    private val state = ChapterState(null, mutableListOf())

    fun currentChapter() = state.currentChapter

    fun startChapter(chapter: Chapter?) {
        if (state.currentChapter != null) {
            state.pastChapters += state.currentChapter !!
        }
        state.currentChapter = chapter
    }

    fun chapterHistory() =
        state.pastChapters
            .groupBy { it.startTime().format(ofPattern("EEE, MMM dd")) }
            .map { dayEntry -> "On " + dayEntry.key+":\n" + dayEntry.value
                .map { it.startTime().format(ofPattern("HH:mm")) + " " + it.title }
                .joinToString("\n")}
            .joinToString("\n\n")

    override fun getState() = state

    override fun loadState(state: ChapterState) = XmlSerializerUtil.copyBean(state, this.state)

    data class ChapterState(var currentChapter: Chapter? = null, val pastChapters :MutableList<Chapter> = mutableListOf())
}

