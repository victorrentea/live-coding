package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ofPattern
import javax.swing.*


class ChapterTitleAction : DumbAwareAction(), CustomComponentAction, WindowFocusListener {
    private val EMPTY_TITLE = "Chapter..."
    private var registeredFocusListener = false
    var stayOnTopFrame: JFrame? = null

    override fun windowGainedFocus(e: WindowEvent?) {
        if (e == null) return
        if (stayOnTopFrame != null) {
            stayOnTopFrame!!.isVisible = false
            stayOnTopFrame!!.dispose()
            stayOnTopFrame = null
        }
    }

    override fun windowLostFocus(e: WindowEvent?) {
        if (e == null) return
        if (e.oppositeWindow == null) {
            val text = service<ChapterService>().currentChapter.formatForStayOnTop()
            stayOnTopFrame = ChapterOnTopFrame(text ?: return)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val oldTitle = service<ChapterService>().currentChapter.title
        val textField = JTextField(oldTitle)

        val builder = DialogBuilder(e.project)
        builder.setCenterPanel(textField)
        builder.setTitle("Chapter")
        builder.removeAllActions()
        builder.addOkAction().setText("Start")
        builder.addCancelAction().setText("Cancel")

        val result = builder.show()

        val button = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) as JButton
        if (result != DialogWrapper.OK_EXIT_CODE) return

        if (textField.text != "" && textField.text != null) {
            // TODO broadcast an event so if many windows are open, the chapter name remains in sync
            logger<ChapterTitleAction>().debug("Previous chapter in service : " + service<ChapterService>().currentChapter)
            service<ChapterService>().currentChapter = Chapter(textField.text)

            button.text = service<ChapterService>().currentChapter.formatForToolbar()
            button.background = Color.yellow

            if (!registeredFocusListener) {
                val frame = WindowManager.getInstance().getFrame(e.project) ?: return
                frame.addWindowFocusListener(this)
                registeredFocusListener = true
            }
        } else {
            button.text = EMPTY_TITLE
            button.background = null
        }
    }



    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val startText = service<ChapterService>().currentChapter.formatForToolbar()
        val button = JButton(startText)
        button.isFocusable = false
        button.toolTipText = "Set current chapter title"
        button.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                actionPerformed(
                    AnActionEvent.createFromInputEvent(
                        e,
                        "GrepConsole-Tail-$place",
                        presentation, DataManager.getInstance().getDataContext(e.getComponent())
                    )
                )
            }
        })
        return button
    }
}