package com.github.victorrentea.livecoding.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class AppSettingsComponent {
    fun getPreferredFocusedComponent() = staticImportsTextArea

    val panel: JPanel

    private val staticImportsTextArea = JBTextArea(30, 10)
    private val showTestResultsSplashCheckbox = JBCheckBox()
    private val playTestResultsSoundCheckbox = JBCheckBox()
    private val reportOpenFileToAddonCheckbox = JBCheckBox()
    private val addonReportUrlField = JBTextField()

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
    var playTestResultsSound: Boolean
        get() = playTestResultsSoundCheckbox.isSelected
        set(newValue) {
            playTestResultsSoundCheckbox.isSelected = newValue
        }

    var reportOpenFileToAddon: Boolean
        get() = reportOpenFileToAddonCheckbox.isSelected
        set(newValue) {
            reportOpenFileToAddonCheckbox.isSelected = newValue
        }
    var addonReportUrl: String
        get() = addonReportUrlField.text
        set(newValue) {
            addonReportUrlField.text = newValue
        }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Show test results splash: "), showTestResultsSplashCheckbox)
            .addLabeledComponent(JBLabel("Play test results sounds: "), playTestResultsSoundCheckbox)
            .addLabeledComponent(JBLabel("Report open file to add-on: "), reportOpenFileToAddonCheckbox)
            .addLabeledComponent(JBLabel("Add-on report URL: "), addonReportUrlField)
            .addSeparator()
            .addLabeledComponentFillVertically("Methods or constants to auto-statically import:",
                JBScrollPane(staticImportsTextArea))
            .panel
    }
}