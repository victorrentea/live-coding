package com.github.victorrentea.livecoding.settings

import com.github.victorrentea.livecoding.tokens.TokenCountService
import com.github.victorrentea.livecoding.tokens.TokenHighlighter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import javax.swing.JComponent


class SettingsConfigurable: Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings: AppSettingsState = AppSettingsState.getInstance()
        if (mySettingsComponent!!.staticImports != settings.staticImportsList) return true
        if (mySettingsComponent!!.showTestResultsSplash != settings.showTestResultsSplash) return true
        if (mySettingsComponent!!.playTestResultsSound != settings.playTestResultsSound) return true
        if (mySettingsComponent!!.reportOpenFileToAddon != settings.reportOpenFileToAddon) return true
        if (mySettingsComponent!!.addonReportUrl != settings.addonReportUrl) return true
        if (mySettingsComponent!!.tokenizerFamily != settings.tokenizerFamily) return true
        if (mySettingsComponent!!.highlightTokens != settings.highlightTokens) return true
        return false
    }

    override fun apply() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
        settings.staticImportsList = mySettingsComponent!!.staticImports
        settings.showTestResultsSplash = mySettingsComponent!!.showTestResultsSplash
        settings.playTestResultsSound = mySettingsComponent!!.playTestResultsSound
        settings.reportOpenFileToAddon = mySettingsComponent!!.reportOpenFileToAddon
        settings.addonReportUrl = mySettingsComponent!!.addonReportUrl
        settings.tokenizerFamily = mySettingsComponent!!.tokenizerFamily
        settings.highlightTokens = mySettingsComponent!!.highlightTokens
        // The status bar and the editor stripes must follow the new settings right away.
        ProjectManager.getInstance().openProjects.forEach { TokenCountService.getInstance(it).refresh() }
        if (!settings.highlightTokens) TokenHighlighter.clearAll()
    }

    override fun getDisplayName() = "Live-Coding"

    override fun disposeUIResources() {
        mySettingsComponent = null;
    }
    override fun reset() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
        mySettingsComponent!!.staticImports = settings.staticImportsList
        mySettingsComponent!!.showTestResultsSplash = settings.showTestResultsSplash
        mySettingsComponent!!.playTestResultsSound = settings.playTestResultsSound
        mySettingsComponent!!.reportOpenFileToAddon = settings.reportOpenFileToAddon
        mySettingsComponent!!.addonReportUrl = settings.addonReportUrl
        mySettingsComponent!!.tokenizerFamily = settings.tokenizerFamily
        mySettingsComponent!!.highlightTokens = settings.highlightTokens
    }
    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.getPreferredFocusedComponent();
    }
}

