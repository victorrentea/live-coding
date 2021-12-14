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
    fun getPreferredFocusedComponent(): JComponent = staticImportsTextArea

    val panel: JPanel
//    private val myUserNameText = JBTextField()
    private val staticImportsTextArea = JBTextArea(100, 10)
//    private val myIdeaUserStatus = JBCheckBox("Do you use IntelliJ IDEA? ")

//    var userNameText: String?
//        get() = myUserNameText.text
//        set(newText) {
//            myUserNameText.text = newText
//        }
//
//    var ideaUserStatus: Boolean
//        get() = myIdeaUserStatus.isSelected
//        set(newStatus) {
//            myIdeaUserStatus.isSelected = newStatus
//        }

    var staticImports: List<String>
        get() = staticImportsTextArea.text.lines()
        set(newList) {
            staticImportsTextArea.text = newList.joinToString("\n")
        }

    init {
        panel = FormBuilder.createFormBuilder()
//            .addLabeledComponent(JBLabel("Enter user nameX: "), myUserNameText, 1, false)
//            .addComponent(myIdeaUserStatus, 1)
            .addLabeledComponentFillVertically("Methods or constants to auto-statically import:", staticImportsTextArea)
            .panel
    }
}