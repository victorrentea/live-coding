package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

//@State(name = "ChapterService",  storages = [Storage("chapter-data.xml")])
class ChapterService/* : PersistentStateComponent<ChapterService>*/ {
    var currentChapter: Chapter = Chapter.NoChapter
        set(value) {
            pastChapters += currentChapter
            field = value
        }
    var pastChapters = mutableListOf<Chapter>()

//    override fun getState() = this
//
//    override fun loadState(state: ChapterService) = XmlSerializerUtil.copyBean(state, this)
}

