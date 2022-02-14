package com.github.victorrentea.livecoding.ux

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JTextField

class TitleAction : DumbAwareAction(), CustomComponentAction {
    private val EMPTY_TITLE = "Title..."
    override fun actionPerformed(e: AnActionEvent) {
        val button = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY) as JButton

        val field = JTextField(if (button.text == EMPTY_TITLE) "" else button.text)

        val builder = DialogBuilder(e.project)
        builder.setCenterPanel(field)
        builder.setTitle("Chapter")
        builder.removeAllActions()
        builder.addCloseButton()
        builder.addCancelAction()
        builder.addOkAction()
        builder.okAction.setText("Set")
        builder.cancelAction.setText("Clear")

        val result = builder.show()
        if (result == DialogWrapper.OK_EXIT_CODE) {
            button.text = field.text
            button.background = Color.yellow
        } else if (result == DialogWrapper.CANCEL_EXIT_CODE) {
            button.text = EMPTY_TITLE
            button.background = null
        }

    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button = JButton(EMPTY_TITLE)
        button.toolTipText = "Set current section title"
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