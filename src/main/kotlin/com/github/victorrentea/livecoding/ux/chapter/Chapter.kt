package com.github.victorrentea.livecoding.ux.chapter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.format.DateTimeFormatter.ofPattern

class Chapter(var title: String = "TODO", start: LocalDateTime = LocalDateTime.now()) {
    var startTimeStr: String = start.format(ISO_DATE_TIME)

    fun startTime(): LocalDateTime = LocalDateTime.parse(startTimeStr, ISO_DATE_TIME)

}