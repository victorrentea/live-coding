package com.github.victorrentea.livecoding.settings

import com.intellij.openapi.options.Configurable
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
        return false
    }

    override fun apply() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
        settings.staticImportsList = mySettingsComponent!!.staticImports
        settings.showTestResultsSplash = mySettingsComponent!!.showTestResultsSplash
        settings.playTestResultsSound = mySettingsComponent!!.playTestResultsSound
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
    }
    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.getPreferredFocusedComponent();
    }
}

