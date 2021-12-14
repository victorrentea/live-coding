package com.github.victorrentea.livecoding

import com.intellij.ide.CopyPasteManagerEx
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.util.ui.TextTransferable
import git4idea.branch.GitBranchUtil
import org.jetbrains.annotations.NotNull

class CopyGitCoordinates : AnAction(){

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { copyGitCoordinatesToClipboard(it) }
    }

    private fun copyGitCoordinatesToClipboard (project: Project) {
        val currentRepository = GitBranchUtil.getCurrentRepository(project) ?: return
        val remoteUrl = currentRepository.remotes.firstOrNull()?.pushUrls?.firstOrNull() ?: return
        val branchName = currentRepository.currentBranch?.name ?: return
        val text = "Git: $remoteUrl\nBranch: $branchName"

        CopyPasteManagerEx.getInstanceEx().setContents(TextTransferable(text as @NotNull CharSequence))
        Notifications.Bus.notify(Notification("Branch Context group", "Git coordinates copied",text, NotificationType.INFORMATION))
    }

}