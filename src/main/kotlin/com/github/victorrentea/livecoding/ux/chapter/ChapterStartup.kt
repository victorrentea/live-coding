package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.WindowManager
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame

/**
 * Shows the current chapter in an always-on-top frame while the IDE is in the background.
 *
 * The AWT focus listener is attached to the *IDE's* frame, which outlives the plugin, so it must be
 * removed when the plugin is unloaded - otherwise the stale listener keeps the old plugin
 * classloader alive ("Some plugins didn't unload fully") and then blows up with a
 * ClassCastException when it calls into the freshly loaded ChapterService.
 * Owning it from a project-level [Service] gives us exactly that: plugin unload disposes the
 * service, and [dispose] detaches the listener.
 */
@Service(Service.Level.PROJECT)
class ChapterFocusTracker : WindowFocusListener, Disposable {
    private var stayOnTopFrame: JFrame? = null
    private var ideFrame: JFrame? = null

    fun attachTo(frame: JFrame) {
        ideFrame = frame
        frame.addWindowFocusListener(this)
    }

    override fun windowGainedFocus(e: WindowEvent?) {
        if (e == null) return
        closeStayOnTopFrame()
    }

    override fun windowLostFocus(e: WindowEvent?) {
        if (e == null) return
        if (e.oppositeWindow == null) {
            val chapter = service<ChapterService>().currentChapter() ?: return
            stayOnTopFrame = ChapterOnTopFrame(chapter)
        }
    }

    override fun dispose() {
        ideFrame?.removeWindowFocusListener(this)
        ideFrame = null
        closeStayOnTopFrame()
    }

    private fun closeStayOnTopFrame() {
        stayOnTopFrame?.let {
            it.isVisible = false
            it.dispose()
        }
        stayOnTopFrame = null
    }
}

class ChapterStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        val frame = WindowManager.getInstance().getFrame(project) ?: return
        project.service<ChapterFocusTracker>().attachTo(frame)
    }
}
