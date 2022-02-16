package com.github.victorrentea.livecoding.ux.chapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ofPattern

data class Chapter(var title: String? = null, var start: LocalDateTime = LocalDateTime.now()) {
    companion object {
        val NoChapter = Chapter(null)
    }

    fun formatForToolbar() = if (title == null) "Chapter..." else start.format(ofPattern("HH:mm")) + " " + title

    fun formatForStayOnTop() = title

    fun formatForHistory() =
        if (title == null) null else start.format(ofPattern("ddd, mmm dd 'at' HH:mm")) + " " + title
}