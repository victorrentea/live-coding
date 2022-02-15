package com.github.victorrentea.livecoding.complexity

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile

class ComplexityRendererStartup : StartupActivity {
    override fun runActivity(project: Project) {
        val handler = MyFileEditorManagerListener(project)

        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, handler)

        ProjectManager.getInstance().addProjectManagerListener(project, object : ProjectManagerListener {
            override fun projectClosing(project: Project) = handler.dispose()
        })
    }

    class MyFileEditorManagerListener(private val project: Project) : FileEditorManagerListener, Disposable {
        private val filesToDispose = mutableMapOf<String, Disposable>()

        init {
            val preOpenEditors = FileEditorManager.getInstance(project).allEditors
            preOpenEditors.filterIsInstance<TextEditor>().forEach { installInlayHandler(it) }
        }

        override fun fileOpened(fileEditorManager: FileEditorManager, file: VirtualFile) {
            fileEditorManager.getEditors(file).filterIsInstance<TextEditor>().forEach { installInlayHandler(it) }
        }

        private fun installInlayHandler(textEditor: TextEditor) {
            val editor = textEditor.editor
            val thisEditorFileUrl = textEditor.file?.url ?: return

            project.messageBus.connect(this)
                .subscribe(RenderComplexityNotifier.CHANGE_ACTION_TOPIC, object : RenderComplexityNotifier {
                    override fun complexityComputed(
                        virtualFileUrl: String,
                        complexities: List<ComplexityToDisplayInFile>
                    ) {
                        if (thisEditorFileUrl == virtualFileUrl) {
                            ApplicationManager.getApplication().invokeLater {
                                // remove previous markup
                                editor.markupModel.allHighlighters
                                    .filter { it.customRenderer is ComplexityHighlightRenderer }
                                    .forEach { editor.markupModel.removeHighlighter(it) }


                                // add new markup
                                for (complexityToDisplay in complexities) {
                                    val highlighter: RangeHighlighter = editor.markupModel.addRangeHighlighter(
                                        null,
                                        complexityToDisplay.offsetInFile,
                                        complexityToDisplay.offsetInFile,
                                        HighlighterLayer.LAST - 1,
                                        HighlighterTargetArea.LINES_IN_RANGE
                                    )
                                    highlighter.customRenderer = ComplexityHighlightRenderer(complexityToDisplay.complexity)
                                }
                            }
                        }
                    }
                })
        }

        override fun dispose() {
            filesToDispose.values.forEach { it.dispose() }
            filesToDispose.clear()
        }
    }
}



