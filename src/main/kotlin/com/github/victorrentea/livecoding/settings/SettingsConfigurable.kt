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
//        if (mySettingsComponent!!.hardCoreImageBackgroundPath != settings.hardCoreImageBackgroundPath) return true
        if (mySettingsComponent!!.staticImports != settings.staticImportsList) return true
        return false
    }

    override fun apply() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
//        settings.userId = mySettingsComponent!!.userNameText!!
//        settings.ideaStatus = mySettingsComponent!!.ideaUserStatus
//        settings.hardCoreImageBackgroundPath = mySettingsComponent!!.hardCoreImageBackgroundPath
        settings.staticImportsList = mySettingsComponent!!.staticImports
    }

    override fun getDisplayName() = "Live-Coding"

    override fun disposeUIResources() {
        mySettingsComponent = null;
    }
    override fun reset() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
        mySettingsComponent!!.staticImports = settings.staticImportsList
//        mySettingsComponent!!.hardCoreImageBackgroundPath = settings.hardCoreImageBackgroundPath
    }
    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.getPreferredFocusedComponent();
    }
}

