package com.github.victorrentea.livecoding.autoreload

import com.github.victorrentea.livecoding.settings.AppSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reloads files changed on disk about a second after the change, with no click.
 *
 * Why the IDE needs this: its native file watcher only *marks* the changed files dirty. The scan
 * that turns those marks into reloaded files runs when the IDE frame is activated (or on File |
 * Reload All from Disk). While the IDE keeps the focus - an agent editing from the IDE's own
 * terminal - or sits visible on a second monitor, nothing triggers it, so editors and the project
 * tree show stale content.
 *
 * So once a second we start the same asynchronous scan frame activation does. With the watcher
 * running it only visits the files the watcher marked, so an idle tick costs next to nothing. If
 * a scan ever turns out slow (no watcher: the whole tree is walked), the next one waits ten times
 * as long as it took, keeping the cost under ~10% of one core.
 *
 * Application-level; [Disposable] so the ticker and the listener go away on dynamic plugin unload.
 */
@Service(Service.Level.APP)
class AutoReloadFromDiskService : Disposable {
    private val log = thisLogger()
    private val scanning = AtomicBoolean(false)
    @Volatile private var scanStartedNanos = 0L
    @Volatile private var nextScanNotBeforeNanos = 0L
    @Volatile private var lastLogNanos = 0L
    private var ticker: ScheduledFuture<*>? = null

    init {
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) = logPickedUp(events)
            })
    }

    @Synchronized
    fun start() {
        if (ticker != null || ApplicationManager.getApplication().isUnitTestMode) return
        ticker = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay({ tick() }, PERIOD_MILLIS, PERIOD_MILLIS, TimeUnit.MILLISECONDS)
    }

    /** Starts a scan unless one is still running, the setting is off, or the last one was slow. */
    fun tick() {
        try {
            if (!AppSettingsState.getInstance().autoReloadFromDisk) return
            if (ProjectManager.getInstance().openProjects.isEmpty()) return
            if (System.nanoTime() < nextScanNotBeforeNanos) return
            if (!scanning.compareAndSet(false, true)) return
            scanStartedNanos = System.nanoTime()
            // Asynchronous, off the EDT, non-modal: changes are applied like after frame activation,
            // including the usual "file changed on disk and in memory" question for unsaved editors.
            VirtualFileManager.getInstance().asyncRefresh {
                val took = System.nanoTime() - scanStartedNanos
                nextScanNotBeforeNanos = System.nanoTime() + took * SLOW_SCAN_BACKOFF_FACTOR
                scanning.set(false)
            }
        } catch (e: Exception) {
            scanning.set(false)
            log.warn("Auto-reload from disk: scan failed", e)
        }
    }

    /** One INFO line (at most every [LOG_EVERY_SECONDS] s) when our scan found external changes. */
    private fun logPickedUp(events: List<VFileEvent>) {
        if (!scanning.get()) return
        val external = events.filter { it.isFromRefresh }
        if (external.isEmpty()) return
        val now = System.nanoTime()
        if (now - lastLogNanos < TimeUnit.SECONDS.toNanos(LOG_EVERY_SECONDS)) return
        lastLogNanos = now
        log.info("Auto-reload from disk: picked up ${external.size} external change(s), e.g. ${external.first().path}")
    }

    override fun dispose() {
        ticker?.cancel(false)
        ticker = null
    }

    companion object {
        private const val PERIOD_MILLIS = 1000L
        private const val SLOW_SCAN_BACKOFF_FACTOR = 10
        private const val LOG_EVERY_SECONDS = 10L
    }
}

class AutoReloadFromDiskStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        service<AutoReloadFromDiskService>().start()
    }
}
