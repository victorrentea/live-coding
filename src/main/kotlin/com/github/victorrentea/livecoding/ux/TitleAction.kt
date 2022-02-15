package com.github.victorrentea.livecoding.ux

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
import java.awt.event.MouseListener
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.*


class TitleAction : DumbAwareAction(), CustomComponentAction, WindowFocusListener {
    private val EMPTY_TITLE = "Title..."
    private var registeredFocusListener = false
    var text: String? = null

    var chapterOnTop: JFrame? = null

    override fun windowGainedFocus(e: WindowEvent?) {
        if (e == null) return
        if (chapterOnTop != null) {
            chapterOnTop!!.isVisible = false
            chapterOnTop!!.dispose()
            chapterOnTop = null
        }
    }

    override fun windowLostFocus(e: WindowEvent?) {
        if (e == null) return
        if (text == null) return
        if (e.oppositeWindow == null) {
            chapterOnTop = JFrame().let {
                it.isAlwaysOnTop = true
                it.isUndecorated = true
                it.background = Color(0, 0, 0, 0)
                it.focusableWindowState = false
                it.isFocusable = false
                it.contentPane = TranslucentPane()

                val b = JButton(text)
                var tEnter: Long = 0
                var yLocationTop = true
                b.addMouseListener(object:MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent?) {
                        tEnter = System.currentTimeMillis()
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        val timeHovered = System.currentTimeMillis() - tEnter
                        if (timeHovered > 1000) {
                            yLocationTop = !yLocationTop
                            if (yLocationTop) {
                                it.location = Point(it.location.x, 0)
                            } else {
                                it.location = Point(it.location.x, Toolkit.getDefaultToolkit().screenSize.height-it.size.height)
                            }
                            it.repaint()
                        }
                    }
                })
                b.background = Color.yellow
                it.add(b)
                it.size = Dimension(b.size)

                it.isVisible = true
                val x = (Toolkit.getDefaultToolkit().screenSize.width - it.size.width) / 2
                it.location = Point(x, 0)
                it.pack()
                it
            }
        }
    }

    class TranslucentPane : JPanel() {
        init {
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g.create() as Graphics2D
            g2d.composite = AlphaComposite.SrcOver.derive(0f)
            g2d.color = background
            g2d.fillRect(0, 0, width, height)
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