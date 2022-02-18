package com.github.victorrentea.livecoding.ux.chapter

import com.intellij.ide.CopyPasteManagerEx
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.TextTransferable
import org.jetbrains.annotations.NotNull
import java.time.LocalDateTime
import javax.swing.*


class ChapterToolbarAction : DumbAwareAction(), CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        actionPerformed(project)
    }

    private fun actionPerformed(project: Project) {
        val oldTitle = service<ChapterService>().currentChapter()?.title

        val textField = JTextField(oldTitle, 15)
        val builder = DialogBuilder(project)
        builder.setCenterPanel(textField)
        builder.setTitle("Chapter")
        builder.removeAllActions()
        builder.addOkAction().setText("Start")
        builder.addCancelAction().setText("Cancel")
        if (builder.show() != DialogWrapper.OK_EXIT_CODE) return
        val newChapterTitle = textField.text

        val newChapter = Chapter(newChapterTitle, LocalDateTime.now())
//        project.messageBus.syncPublisher(ChapterNotifier.TOPIC).chapterChanged(newChapter)

        service<ChapterService>().startChapter(newChapter)
    }


    override fun update(e: AnActionEvent) {
        val currentChapter = service<ChapterService>().currentChapter()

        // chapter from yesterday
        if (currentChapter?.startTime()?.dayOfMonth != LocalDateTime.now().dayOfMonth) {
            service<ChapterService>().startChapter(null)
        }

        e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
            .castSafelyTo<ChapterToolbarComponent>()
            ?.setChapter(currentChapter)
    }

    override fun createCustomComponent(presentation: Presentation, place: String) =
        ChapterToolbarComponent { actionPerformed(it) }
}


class ChapterClearToolbarAction: DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        service<ChapterService>().startChapter(null)
    }

}
class ChapterHistoryToolbarAction: DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val historyStr = service<ChapterService>().chapterHistory()
        CopyPasteManagerEx.getInstanceEx().setContents(TextTransferable(historyStr as @NotNull CharSequence))
        Notifications.Bus.notify(Notification("Branch Context group", "Chapter History copied to clipboard",historyStr, NotificationType.INFORMATION))
    }
}