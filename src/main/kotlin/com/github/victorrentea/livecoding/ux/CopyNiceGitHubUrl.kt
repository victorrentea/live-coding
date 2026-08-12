package com.github.victorrentea.livecoding.ux

import com.intellij.ide.CopyPasteManagerEx
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.ui.TextTransferable
import git4idea.repo.GitRemote
import git4idea.repo.GitRepositoryManager
import org.jetbrains.annotations.NotNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * "Copy Nice GitHub url": the GitHub link to the selected file **on the current branch**
 * (`.../blob/main/CLAUDE.md`), instead of the pinned-to-a-commit-SHA link that
 * *GitHub | Open in Browser* produces (`.../blob/1403c0e9c33.../CLAUDE.md`).
 */
class CopyNiceGitHubUrl : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = niceUrl(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val url = niceUrl(e) ?: return
        CopyPasteManagerEx.getInstanceEx().setContents(TextTransferable(url as @NotNull CharSequence))
        Notifications.Bus.notify(
            Notification("Branch Context group", "GitHub URL copied", url, NotificationType.INFORMATION)
        )
    }

    private fun niceUrl(e: AnActionEvent): String? {
        val project = e.project ?: return null
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val repositoryManager = GitRepositoryManager.getInstance(project)
        val repository = file?.let { repositoryManager.getRepositoryForFileQuick(it) }
            ?: repositoryManager.repositories.singleOrNull()
            ?: return null

        // a local branch is a nicer, longer-living link than the commit SHA; fall back to the SHA when detached
        val ref = repository.currentBranch?.name ?: repository.currentRevision ?: return null
        val remoteUrls = repository.remotes
            .sortedBy { if (it.name == GitRemote.ORIGIN) 0 else 1 }
            .flatMap { it.urls }
        val relativePath = file
            ?.takeIf { it != repository.root }
            ?.let { VfsUtilCore.getRelativePath(it, repository.root, '/') }

        return niceGitHubUrl(remoteUrls, ref, relativePath, file?.isDirectory ?: true)
    }
}

private val GITHUB_REMOTE_URL =
    Regex("""^(?:(?:https?|ssh|git)://)?(?:[^@/]+@)?([^:/]+)(?::\d+(?=/))?[:/]([^/]+)/(.+)$""")

/** `https://github.com/owner/repo/blob/main/some/File.java`, or null if no remote points to GitHub. */
internal fun niceGitHubUrl(
    remoteUrls: List<String>,
    ref: String,
    relativePath: String?,
    isDirectory: Boolean
): String? {
    val webUrl = remoteUrls.firstNotNullOfOrNull(::gitHubWebUrl) ?: return null
    if (relativePath.isNullOrEmpty()) return "$webUrl/tree/${encodePath(ref)}"
    val kind = if (isDirectory) "tree" else "blob"
    return "$webUrl/$kind/${encodePath(ref)}/${encodePath(relativePath)}"
}

/** Any remote flavour (https, ssh, scp-like `git@…:owner/repo.git`) → the browsable repo URL. */
internal fun gitHubWebUrl(remoteUrl: String): String? {
    val cleaned = remoteUrl.trim().trimEnd('/').removeSuffix(".git")
    val (host, owner, repo) = GITHUB_REMOTE_URL.find(cleaned)?.destructured ?: return null
    if (!host.lowercase().contains("github")) return null // also covers GitHub Enterprise hosts
    return "https://$host/$owner/$repo"
}

private fun encodePath(path: String) = path.split('/')
    .joinToString("/") { URLEncoder.encode(it, StandardCharsets.UTF_8).replace("+", "%20") }
