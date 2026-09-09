package com.github.victorrentea.livecoding.drawio

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.fileTypes.NativeFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * A `*.drawio.png` is a PNG that carries the diagram source embedded in it, so the IDE image
 * viewer renders it but can only ever show it read-only. Typing those files as an
 * [INativeFileType] makes the platform hand the file to the desktop app instead of opening a
 * preview tab: navigation checks `instanceof INativeFileType` before it ever reaches an editor
 * (`FileNavigatorImpl`), so a double click in the Project view lands straight in draw.io.
 */
object DrawioPngFileType : INativeFileType {
    /** draw.io Desktop's bundle id — stable across versions, unlike the "draw.io.app" file name. */
    private const val MAC_BUNDLE_ID = "com.jgraph.drawio.desktop"

    /** `open` only asks LaunchServices to start the app, it does not wait for it to come up. */
    private const val LAUNCH_TIMEOUT_MS = 10_000

    override fun getName() = "Draw.io PNG"
    override fun getDescription() = "Draw.io diagram embedded in a PNG"
    override fun getDefaultExtension() = "drawio.png"
    override fun getIcon(): Icon = AllIcons.FileTypes.Diagram
    override fun isBinary() = true

    /** The OS icon for a `.drawio.png` is whatever is registered for PNG — the diagram icon says more. */
    override fun useNativeIcon() = false

    override fun openFileInAssociatedApplication(project: Project?, file: VirtualFile): Boolean {
        // Inside a jar, or over http: there is no path to hand an external app. Let the viewer have it.
        if (!file.isInLocalFileSystem) return false

        // Launching off the EDT: the click returns immediately and draw.io comes up on its own.
        ApplicationManager.getApplication().executeOnPooledThread { openInDrawio(project, file) }
        return true
    }

    private fun openInDrawio(project: Project?, file: VirtualFile) {
        if (SystemInfo.isMac && launch("open", "-b", MAC_BUNDLE_ID, file.presentableUrl)) return

        // Not a Mac, or draw.io Desktop is not installed here: fall back to the OS association,
        // which is what the file would have got from the platform's own native file type.
        if (NativeFileType.openAssociatedApplication(file)) return

        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(
                "Could not open ${file.name} in draw.io",
                "Install draw.io Desktop, or associate .png files with an application.",
                NotificationType.WARNING
            )
            .notify(project)
    }

    private fun launch(vararg command: String): Boolean =
        try {
            CapturingProcessHandler(GeneralCommandLine(*command))
                .runProcess(LAUNCH_TIMEOUT_MS)
                .exitCode == 0
        } catch (e: ExecutionException) {
            thisLogger().warn("Failed to launch: ${command.joinToString(" ")}", e)
            false
        }

    /** Must match the `notificationGroup` id declared in plugin.xml. */
    private const val NOTIFICATION_GROUP = "Live-Coding"
}
