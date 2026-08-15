package com.github.victorrentea.livecoding.settings

import com.github.victorrentea.livecoding.tokens.TokenizerFamily
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
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
    private val tokenizerFamilyCombo = ComboBox(TokenizerFamily.values()).apply {
        renderer = SimpleListCellRenderer.create("") { "${it.displayName} - ${it.note}" }
    }
    private val highlightTokensCheckbox = JBCheckBox()

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

    var tokenizerFamily: TokenizerFamily
        get() = tokenizerFamilyCombo.item
        set(newValue) {
            tokenizerFamilyCombo.item = newValue
        }
    var highlightTokens: Boolean
        get() = highlightTokensCheckbox.isSelected
        set(newValue) {
            highlightTokensCheckbox.isSelected = newValue
        }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Show test results splash: "), showTestResultsSplashCheckbox)
            .addLabeledComponent(JBLabel("Play test results sounds: "), playTestResultsSoundCheckbox)
            .addLabeledComponent(JBLabel("Report open file to add-on: "), reportOpenFileToAddonCheckbox)
            .addLabeledComponent(JBLabel("Add-on report URL: "), addonReportUrlField)
            .addSeparator()
            .addLabeledComponent(JBLabel("Count tokens for: "), tokenizerFamilyCombo)
            .addLabeledComponent(JBLabel("Highlight token boundaries in the editor: "), highlightTokensCheckbox)
            .addSeparator()
            .addLabeledComponentFillVertically("Methods or constants to auto-statically import:",
                JBScrollPane(staticImportsTextArea))
            .panel
    }
}