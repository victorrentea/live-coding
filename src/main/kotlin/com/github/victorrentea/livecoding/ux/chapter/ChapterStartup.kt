package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.WindowManager
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame

class ChapterStartup : StartupActivity, WindowFocusListener {
    private var stayOnTopFrame: JFrame? = null
    override fun runActivity(project: Project) {
        val frame = WindowManager.getInstance().getFrame(project)
        println("Frame: " + frame)
        frame?.addWindowFocusListener(this)
    }

    override fun windowGainedFocus(e: WindowEvent?) {
        if (e == null) return
        if (stayOnTopFrame != null) {
//            println("Gainedfocus ")
            stayOnTopFrame!!.isVisible = false
            stayOnTopFrame!!.dispose()
            stayOnTopFrame = null
        }
    }

    override fun windowLostFocus(e: WindowEvent?) {
//            println("Lost focus FIRST")
        if (e == null) return
        if (e.oppositeWindow == null) {
//            println("Lost focus")
            val chapter = service<ChapterService>().currentChapter() ?: return
            stayOnTopFrame = ChapterOnTopFrame(chapter)
        }
    }
}



