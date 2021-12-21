package com.github.victorrentea.livecoding.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class AppSettingsComponent {
    fun getPreferredFocusedComponent() = staticImportsTextArea

    val panel: JPanel

    private val mood1NameField = JBTextField(10)
    private val mood1FileNameField = JBTextField(100)
    private val mood2NameField = JBTextField(10)
    private val mood2FileNameField = JBTextField(100)
    private val staticImportsTextArea = JBTextArea(100, 10)

//    var hardCoreImageBackgroundPath: String?
//        get() = mood1FileNameField.text
//        set(newText) {
//            mood1FileNameField.text = newText
//        }

    var staticImports: List<String>
        get() = staticImportsTextArea.text.lines()
        set(newList) {
            staticImportsTextArea.text = newList.joinToString("\n")
        }

    init {
//        val mood1 = JBPanel
        panel = FormBuilder.createFormBuilder()
//            .addLabeledComponent(JBLabel("Mood #1 Name:"), JBPanel() mood1NameField, 1, false)
//            .addLabeledComponent(JBLabel("Mood #2 Name:"), mood2NameField, 1, false)

//            .addLabeledComponent(JBLabel("Path to use for background when entering hard-core mode: "), mood1FileNameField, 1, false)
//            .addComponent(myIdeaUserStatus, 1)
            .addSeparator()
            .addLabeledComponentFillVertically("Methods or constants to auto-statically import:", staticImportsTextArea)
            .panel
    }
}