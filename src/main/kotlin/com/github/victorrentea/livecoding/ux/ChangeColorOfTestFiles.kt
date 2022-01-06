package com.github.victorrentea.livecoding.ux

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.util.ui.UIUtil
import java.awt.Color

class ChangeColorOfTestFiles : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val project = event.editor.project ?: return
        val document = event.editor.document ?: return
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val fileIndex = ProjectFileIndex.SERVICE.getInstance(project)

        if (virtualFile?.let {fileIndex .getSourceFolder(virtualFile)?.isTestSource } == true) {
            val color = if (UIUtil.isUnderDarcula()) Color(73, 84, 74) else Color(239, 250, 231)
            event.editor.colorsScheme.setColor(EditorColors.GUTTER_BACKGROUND, color)
        }

    }
}
