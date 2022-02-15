package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.*


class ChapterTitleAction : DumbAwareAction(), CustomComponentAction, WindowFocusListener {
    private val EMPTY_TITLE = "Chapter..."
    private var registeredFocusListener = false
    var text: String? = null

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
            stayOnTopFrame = ChapterOnTopFrame(text ?: return)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val textField = JTextField(text)

        val builder = DialogBuilder(e.project)
        builder.setCenterPanel(textField)
        builder.setTitle("Chapter")
        builder.removeAllActions()
        builder.addOkAction().setText("Set")
        builder.addCancelAction().setText("Cancel")

        val result = builder.show()

        val button = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) as JButton
        if (result == DialogWrapper.OK_EXIT_CODE) {
            if (textField.text != "" && textField.text != null) {
                text = textField.text
                button.text = textField.text
                button.background = Color.yellow
                if (!registeredFocusListener) {
                    val frame = WindowManager.getInstance().getFrame(e.project) ?: return
                    frame.addWindowFocusListener(this)
//                    println("Added focus listener")
                    registeredFocusListener = true
                }
            } else {
                text = null
                button.text = EMPTY_TITLE
                button.background = null
            }
        }
    }


    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button = JButton(EMPTY_TITLE)
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