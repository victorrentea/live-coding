package com.github.victorrentea.livecoding.markdown

import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.ResourceProvider

/**
 * Lets the user zoom the rendered Markdown preview with Ctrl/Cmd + mouse wheel
 * (Cmd/Ctrl + 0 resets). IntelliJ has no built-in wheel-zoom for the preview
 * (YouTrack IJPL-93544), so we hook the official `browserPreviewExtensionProvider`
 * extension point and inject a script the same way the bundled copy-button /
 * PlantUML extensions do.
 *
 * The name in [scripts] is resolved by [loadResource] via
 * `ResourceProvider.loadInternalResource`, which looks the file up *relative to this
 * class's package* — i.e. `com/github/victorrentea/livecoding/markdown/markdown-preview-zoom.js`.
 */
internal class MarkdownPreviewZoomExtension(
    @Suppress("unused") panel: MarkdownHtmlPanel,
) : MarkdownBrowserPreviewExtension, ResourceProvider {

    // Load after the base scripts/styles so our listeners win.
    override val priority: MarkdownBrowserPreviewExtension.Priority =
        MarkdownBrowserPreviewExtension.Priority.AFTER_ALL

    override val scripts: List<String> = listOf(MAIN_SCRIPT)

    override val resourceProvider: ResourceProvider = this

    override fun canProvide(resourceName: String): Boolean = resourceName == MAIN_SCRIPT

    override fun loadResource(resourceName: String): ResourceProvider.Resource? =
        ResourceProvider.loadInternalResource<MarkdownPreviewZoomExtension>(resourceName)

    override fun dispose() = Unit

    class Provider : MarkdownBrowserPreviewExtension.Provider {
        override fun createBrowserExtension(panel: MarkdownHtmlPanel): MarkdownBrowserPreviewExtension =
            MarkdownPreviewZoomExtension(panel)
    }

    companion object {
        private const val MAIN_SCRIPT = "markdown-preview-zoom.js"
    }
}
