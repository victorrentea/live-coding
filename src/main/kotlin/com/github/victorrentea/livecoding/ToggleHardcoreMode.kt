package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.impl.IdeBackgroundUtil


class ToggleHardcoreMode : AnAction() {
    companion object {
        private var on = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        ApplicationManager.getApplication().invokeLater {
            on = !on;
            val prop = PropertiesComponent.getInstance()
            prop.setValue(IdeBackgroundUtil.FRAME_PROP, null);

            val bgImagePath = AppSettingsState.getInstance().hardCoreImageBackgroundPath

            prop.setValue(IdeBackgroundUtil.EDITOR_PROP, if (on) bgImagePath else null);
            IdeBackgroundUtil.repaintAllWindows()
            if (on) {
                editor?.let { editor ->
                    HintManager.getInstance().showErrorHint(editor, "Entering hard-core mode ...")
                }
            }
        }
    }
}