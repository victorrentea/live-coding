package com.github.victorrentea.livecoding.settings

import com.github.victorrentea.livecoding.settings.AppSettingsComponent
import com.github.victorrentea.livecoding.settings.AppSettingsState
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
//        if (mySettingsComponent!!.userNameText != settings.userId) return true
//        if (mySettingsComponent!!.ideaUserStatus != settings.ideaStatus) return true
        if (mySettingsComponent!!.staticImports != settings.staticImportsList) return true
        return false
    }

    override fun apply() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
//        settings.userId = mySettingsComponent!!.userNameText!!
//        settings.ideaStatus = mySettingsComponent!!.ideaUserStatus
        settings.staticImportsList = mySettingsComponent!!.staticImports
    }

    override fun getDisplayName(): String {
        return "Live-Coding Settings"
    }

    override fun disposeUIResources() {
        mySettingsComponent = null;
    }
    override fun reset() {
        val settings: AppSettingsState = AppSettingsState.getInstance()
//        mySettingsComponent!!.userNameText = settings.userId
//        mySettingsComponent!!.ideaUserStatus = settings.ideaStatus
        mySettingsComponent!!.staticImports = settings.staticImportsList
    }
    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.getPreferredFocusedComponent();
    }
}
