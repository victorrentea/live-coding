package com.github.victorrentea.livecoding.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.lang.IllegalArgumentException
import javax.swing.JComponent
import javax.swing.JPanel

class AppSettingsComponent {
    fun getPreferredFocusedComponent() = staticImportsTextArea

    val panel: JPanel

    private val mood1NameField = JBTextField()
    private val hardCoreImageBackground = JBTextField()
    private val staticImportsTextArea = JBTextArea(100, 10)

    var hardCoreImageBackgroundPath: String?
        get() = hardCoreImageBackground.text
        set(newText) {
            hardCoreImageBackground.text = newText
        }

    var staticImports: List<String>
        get() = staticImportsTextArea.text.lines()
        set(newList) {
            staticImportsTextArea.text = newList.joinToString("\n")
        }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Mood #1 Name:"), mood1NameField, 1, false)

            .addLabeledComponent(JBLabel("Path to use for background when entering hard-core mode: "), hardCoreImageBackground, 1, false)
//            .addComponent(myIdeaUserStatus, 1)
            .addSeparator()
            .addLabeledComponentFillVertically("Methods or constants to auto-statically import:", staticImportsTextArea)
            .panel
    }
}