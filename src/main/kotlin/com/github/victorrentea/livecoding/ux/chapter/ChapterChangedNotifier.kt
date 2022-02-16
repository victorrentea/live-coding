package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.util.messages.Topic

interface ChapterChangedNotifier {
    companion object {
        @Topic.AppLevel
        val TOPIC = Topic.create("Chapter Changed", ChapterChangedNotifier::class.java)
    }
    fun onChapterChanged(newChapter: Chapter)
}