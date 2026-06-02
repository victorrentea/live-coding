package com.github.victorrentea.livecoding.openfile

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import git4idea.repo.GitRepositoryManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Reports the file the user is actively looking at to an external add-on, but only after the
 * user dwells [DWELL_SECONDS] on it in a focused IDE window (and only when the setting is on).
 *
 * The add-on (and the daemon behind it) resolve the GitHub link on the repo's default branch,
 * so here we only send the git remote URL + the path relative to the git repo root.
 *
 * Application-level service (one per IDE process; registered in plugin.xml). [Disposable] so the
 * pending dwell task is cancelled cleanly on dynamic plugin unload.
 */
class OpenFileReporter : Disposable {
    private val log = thisLogger()
    private val scheduler = AppExecutorUtil.getAppScheduledExecutorService()

    private val lock = Any()
    private var focusedProject: Project? = null
    private var appActive = false
    private var pending: ScheduledFuture<*>? = null
    private var lastSentKey: String? = null

    /** The IDE window gained focus — start watching its currently selected file. */
    fun ideActivated(project: Project?) {
        synchronized(lock) {
            appActive = true
            focusedProject = project
        }
        if (project != null && !project.isDisposed) {
            candidateChanged(project, selectedFile(project))
        }
    }

    /** The IDE lost OS focus — a half-finished dwell shouldn't fire. */
    fun ideDeactivated() {
        synchronized(lock) {
            appActive = false
            pending?.cancel(false)
            pending = null
        }
    }

    /** The active file changed (tab switch / open) or a window was focused. (Re)start the dwell. */
    fun candidateChanged(project: Project, file: VirtualFile?) {
        if (!AppSettingsState.getInstance().reportOpenFileToAddon) return
        synchronized(lock) {
            pending?.cancel(false)
            pending = if (file == null) null
            else scheduler.schedule({ report(project, file) }, DWELL_SECONDS, TimeUnit.SECONDS)
        }
    }

    private fun report(project: Project, file: VirtualFile) {
        if (project.isDisposed) return
        if (!AppSettingsState.getInstance().reportOpenFileToAddon) return

        // Re-validate the dwell: same focused project, IDE still active, file still selected.
        synchronized(lock) {
            if (!appActive || focusedProject !== project) return
        }
        if (selectedFile(project) != file) return

        val payload = runReadAction { buildPayload(project, file) } ?: return

        val key = "${payload.url}|${payload.file}"
        synchronized(lock) {
            if (key == lastSentKey) return
            lastSentKey = key
        }
        post(payload)
    }

    private fun selectedFile(project: Project): VirtualFile? = runReadAction {
        if (project.isDisposed) null
        else FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
    }

    private fun buildPayload(project: Project, file: VirtualFile): Payload? {
        val repo = GitRepositoryManager.getInstance(project).getRepositoryForFile(file) ?: return null
        val relPath = VfsUtilCore.getRelativePath(file, repo.root, '/') ?: return null
        val remote = repo.remotes.find { it.name == "origin" } ?: repo.remotes.firstOrNull() ?: return null
        val url = remote.firstUrl ?: return null
        val branch = repo.currentBranch?.name ?: ""
        return Payload(url = url, branch = branch, file = relPath, project = project.name)
    }

    private fun post(payload: Payload) {
        val url = AppSettingsState.getInstance().addonReportUrl
        val json = buildString {
            append('{')
            append("\"url\":").append(jsonString(payload.url)).append(',')
            append("\"branch\":").append(jsonString(payload.branch)).append(',')
            append("\"file\":").append(jsonString(payload.file)).append(',')
            append("\"project\":").append(jsonString(payload.project))
            append('}')
        }
        try {
            HttpRequests.post(url, "application/json")
                .connectTimeout(1500)
                .readTimeout(1500)
                .connect { request ->
                    request.write(json)
                    request.readString() // complete the round-trip; ignore the body
                }
            log.debug("reported open file to add-on: ${payload.file}")
        } catch (e: Exception) {
            log.debug("failed to report open file to add-on at $url: ${e.message}")
        }
    }

    override fun dispose() {
        synchronized(lock) {
            pending?.cancel(false)
            pending = null
        }
    }

    private data class Payload(val url: String, val branch: String, val file: String, val project: String)

    companion object {
        private const val DWELL_SECONDS = 5L

        fun getInstance(): OpenFileReporter =
            ApplicationManager.getApplication().getService(OpenFileReporter::class.java)

        private fun jsonString(s: String): String {
            val sb = StringBuilder(s.length + 2)
            sb.append('"')
            for (c in s) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            sb.append('"')
            return sb.toString()
        }
    }
}
