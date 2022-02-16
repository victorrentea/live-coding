package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.util.messages.Topic

interface ChapterNotifier {
    companion object {
        @Topic.AppLevel
        val TOPIC = Topic.create("Chapter Changed", ChapterNotifier::class.java)
    }
    fun chapterChanged(newChapter: Chapter)
}