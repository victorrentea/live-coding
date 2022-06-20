package com.github.victorrentea.livecoding.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class AppSettingsComponent {
    fun getPreferredFocusedComponent() = staticImportsTextArea

    val panel: JPanel

    private val staticImportsTextArea = JBTextArea(100, 10)
    private val showTestResultsSplashCheckbox = JBCheckBox()

    var staticImports: List<String>
        get() = staticImportsTextArea.text.lines()
        set(newList) {
            staticImportsTextArea.text = newList.joinToString("\n")
        }

    var showTestResultsSplash: Boolean
        get() = showTestResultsSplashCheckbox.isSelected
        set(newValue) {
            showTestResultsSplashCheckbox.isSelected = newValue
        }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Show test results splash: "), showTestResultsSplashCheckbox)
            .addSeparator()
            .addLabeledComponentFillVertically("Methods or constants to auto-statically import:", staticImportsTextArea)
            .panel
    }
}