package com.github.victorrentea.livecoding.ux.quickdef

import com.intellij.codeInsight.hint.ImplementationViewComponent
import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.codeInsight.hint.PsiImplementationViewElement
import com.intellij.ide.util.EditSourceUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.Promise
import java.awt.Cursor
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * Makes the Quick Definition popup (`⌥Space` / `Ctrl+Shift+I`) navigable. Its editor shows a plain
 * text copy of the definition, with no PSI behind it, so the IDE's own Cmd+click does nothing there.
 * Each popup offset is mapped back into the source file the fragment was copied from, and resolved there:
 * - **Cmd/Ctrl+click** a token: go to its declaration (the popup closes). Holding Cmd underlines it.
 * - **Quick Definition shortcut** inside the popup: show the definition of the token under the caret
 *   in the same popup ("preview in preview").
 * - **Back shortcut** (`⌘[` / `Ctrl+Alt+←`) inside the popup: return to the previous definition.
 */
class QuickDefinitionEditorListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.editorKind != EditorKind.PREVIEW) return
        val project = editor.project ?: return
        QuickDefinitionNavigation(editor, project).install()
    }
}

internal class QuickDefinitionNavigation(private val editor: Editor, private val project: Project) :
    EditorMouseListener, EditorMouseMotionListener {

    private var hover: RangeHighlighter? = null

    fun install() {
        editor.addEditorMouseListener(this)
        editor.addEditorMouseMotionListener(this)
        drillInAction().registerCustomShortcutSet(shortcutsOf("QuickImplementations"), editor.contentComponent)
        backAction().registerCustomShortcutSet(shortcutsOf(IdeActions.ACTION_GOTO_BACK), editor.contentComponent)
    }

    // ---------------------------------------------------------------- Cmd+click: go to declaration

    // Acts on press, consumed: once a press is consumed the editor no longer reports the click.
    override fun mousePressed(e: EditorMouseEvent) {
        if (!isNavigationClick(e.mouseEvent)) return
        val view = view() ?: return
        e.consume() // no caret move / selection
        clearHover()
        inBackground(view, e.offset, { spot, targets ->
            spot to targets.firstNotNullOfOrNull { EditSourceUtil.getDescriptor(it) }
        }) { (spot, descriptor) ->
            if (spot == null) return@inBackground
            PopupUtil.getPopupContainerFor(view)?.cancel()
            if (descriptor != null && descriptor.canNavigate()) descriptor.navigate(true)
            else openAndGoToDeclaration(spot)
        }
    }

    /** Nothing resolved from the copy: open the source file at that spot and let the IDE's own Go to Declaration try. */
    private fun openAndGoToDeclaration(spot: SourceSpot) {
        val vFile = spot.file.virtualFile ?: return
        val target = FileEditorManager.getInstance(project)
            .openTextEditor(OpenFileDescriptor(project, vFile, spot.offset), true) ?: return
        val action = ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_DECLARATION) ?: return
        ActionUtil.invokeAction(action, target.contentComponent, "QuickDefinitionPopup", null, null)
    }

    // ---------------------------------------------------------------- Cmd+hover: underline the link

    override fun mouseMoved(e: EditorMouseEvent) {
        if (!isNavigationModifier(e.mouseEvent)) return clearHover()
        val view = view() ?: return clearHover()
        val current = hover
        if (current != null && current.isValid && e.offset in current.startOffset until current.endOffset) return
        clearHover()
        val range = referenceRangeInPopup(view, e.offset) ?: return
        hover = editor.markupModel.addRangeHighlighter(
            EditorColors.REFERENCE_HYPERLINK_COLOR, range.startOffset, range.endOffset,
            HighlighterLayer.HYPERLINK, HighlighterTargetArea.EXACT_RANGE
        )
        (editor as? EditorEx)?.setCustomCursor(this, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    }

    override fun mouseExited(e: EditorMouseEvent) = clearHover()

    private fun clearHover() {
        hover?.let { if (it.isValid) editor.markupModel.removeHighlighter(it) }
        hover = null
        (editor as? EditorEx)?.setCustomCursor(this, null)
    }

    private fun referenceRangeInPopup(view: ImplementationViewComponent, popupOffset: Int): TextRange? {
        val shown = view.elements.getOrNull(view.index) ?: return null
        val spot = sourceSpot(shown, editor.document.immutableCharSequence, popupOffset) ?: return null
        val absolute = safeRead {
            val ref = spot.file.findReferenceAt(spot.offset)
            if (ref != null) absoluteRange(ref) else declarationNameAt(spot)?.textRange
        } ?: return null
        val shifted = absolute.shiftLeft(spot.fragmentStart)
        return if (shifted.startOffset >= 0 && shifted.endOffset <= editor.document.textLength) shifted else null
    }

    // ---------------------------------------------------------------- ⌥Space: preview in preview, ⌘[: back

    private fun drillInAction() = object : DumbAwareAction() {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = view() != null
        }

        override fun actionPerformed(e: AnActionEvent) {
            drillIn(editor.caretModel.offset)
        }
    }

    private fun backAction() = object : DumbAwareAction() {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = view()?.let { history(it).isNotEmpty() } == true
        }

        override fun actionPerformed(e: AnActionEvent) {
            back()
        }
    }

    /** Shows, in the same popup, the definition of the token at [popupOffset]. Completes with whether it did. */
    internal fun drillIn(popupOffset: Int): Promise<Boolean>? {
        val view = view() ?: return null
        val before = view.elements.toList() to view.index
        return inBackground(view, popupOffset, { _, targets ->
            targets.map { PsiImplementationViewElement(it) } // must be built off the EDT
        }) { elements ->
            if (elements.isNotEmpty()) {
                history(view).addLast(before)
                view.update(elements, 0)
            }
        }.then { it.isNotEmpty() }
    }

    internal fun back(): Boolean {
        val view = view() ?: return false
        val (elements, index) = history(view).removeLastOrNull() ?: return false
        view.update(elements, index)
        return true
    }

    /** What the token at [popupOffset] resolves to, in the file the popup text was copied from. */
    internal fun targetsAt(popupOffset: Int): CancellablePromise<List<PsiElement>>? {
        val view = view() ?: return null
        return inBackground(view, popupOffset, { _, targets -> targets }) {}
    }

    /**
     * Resolves the token at [popupOffset] under a non-blocking read action (resolving can be slow, and
     * the platform's view elements refuse to be built on the EDT), then hands the result to [onUi].
     */
    private fun <T> inBackground(
        view: ImplementationViewComponent,
        popupOffset: Int,
        compute: (SourceSpot?, List<PsiElement>) -> T,
        onUi: (T) -> Unit,
    ): CancellablePromise<T> {
        val shown = view.elements.getOrNull(view.index)
        val popupText = editor.document.immutableCharSequence
        return ReadAction.nonBlocking<T> {
            val spot = shown?.let { sourceSpot(it, popupText, popupOffset) }
            compute(spot, spot?.let { resolve(it) } ?: emptyList())
        }
            .expireWhen { editor.isDisposed || project.isDisposed }
            .finishOnUiThread(ModalityState.stateForComponent(view), onUi)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    @Suppress("UNCHECKED_CAST")
    private fun history(view: ImplementationViewComponent): ArrayDeque<Pair<List<ImplementationViewElement>, Int>> =
        view.getClientProperty(HISTORY_KEY) as? ArrayDeque<Pair<List<ImplementationViewElement>, Int>>
            ?: ArrayDeque<Pair<List<ImplementationViewElement>, Int>>().also { view.putClientProperty(HISTORY_KEY, it) }

    // ---------------------------------------------------------------- popup offset -> source file offset

    /** The popup this editor sits in; null for every other preview editor (diff, settings, etc). */
    private fun view(): ImplementationViewComponent? =
        SwingUtilities.getAncestorOfClass(ImplementationViewComponent::class.java, editor.component) as? ImplementationViewComponent

    private fun sourceSpot(shown: ImplementationViewElement, popupText: CharSequence, popupOffset: Int): SourceSpot? = safeRead {
        val psi = (shown as? PsiImplementationViewElement)?.getPsiElement() ?: shown.elementForShowUsages
            ?: return@safeRead null
        val file = psi.containingFile?.originalFile ?: return@safeRead null
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return@safeRead null
        val hint = psi.textRange?.let { document.getLineStartOffset(document.getLineNumber(it.startOffset)) } ?: 0
        val start = FragmentLocator.locate(document.charsSequence, popupText, hint)
        if (start < 0) null else SourceSpot(file, start, start + popupOffset)
    }

    private fun resolve(spot: SourceSpot): List<PsiElement> = safeRead {
        val ref = spot.file.findReferenceAt(spot.offset)
        val resolved = when (ref) {
            null -> emptyList()
            is PsiPolyVariantReference -> ref.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(ref.resolve())
        }
        resolved.ifEmpty { listOfNotNull(declarationNameAt(spot)) }.map { it.navigationElement ?: it }.distinct()
    } ?: emptyList()

    /** The clicked token is the name of a declaration (e.g. the method name in its own signature). */
    private fun declarationNameAt(spot: SourceSpot): PsiElement? {
        val leaf = spot.file.findElementAt(spot.offset) ?: return null
        val owner = PsiTreeUtil.getParentOfType(leaf, PsiNameIdentifierOwner::class.java, false) ?: return null
        val name = owner.nameIdentifier ?: return null
        return if (name.textRange.containsOffset(spot.offset)) owner else null
    }

    private fun absoluteRange(ref: PsiReference): TextRange =
        ref.rangeInElement.shiftRight(ref.element.textRange.startOffset)

    private fun <T> safeRead(block: () -> T?): T? = try {
        ReadAction.compute<T?, RuntimeException> { block() }
    } catch (_: IndexNotReadyException) {
        null
    }

    private fun isNavigationModifier(e: MouseEvent): Boolean {
        val mask = if (SystemInfo.isMac) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK
        val others = InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK or
            (if (SystemInfo.isMac) InputEvent.CTRL_DOWN_MASK else InputEvent.META_DOWN_MASK)
        return e.modifiersEx and mask != 0 && e.modifiersEx and others == 0
    }

    private fun isNavigationClick(e: MouseEvent) =
        e.button == MouseEvent.BUTTON1 && e.clickCount == 1 && isNavigationModifier(e)

    private fun shortcutsOf(actionId: String) =
        CustomShortcutSet(*KeymapManager.getInstance().activeKeymap.getShortcuts(actionId))

    private data class SourceSpot(val file: PsiFile, val fragmentStart: Int, val offset: Int)

    private companion object {
        const val HISTORY_KEY = "liveCoding.quickDefinition.history"
    }
}

/** Where the popup's text was copied from inside the source document. */
internal object FragmentLocator {
    /**
     * @param hint where the fragment most likely starts (the line start of the shown element)
     * @return the start of [fragment] in [source], preferring [hint], else the occurrence closest to it; -1 if absent
     */
    fun locate(source: CharSequence, fragment: CharSequence, hint: Int): Int {
        if (fragment.isEmpty()) return -1
        if (hint in 0..source.length - fragment.length && source.regionMatches(hint, fragment, 0, fragment.length)) return hint
        val text = source.toString()
        val needle = fragment.toString()
        var best = -1
        var at = text.indexOf(needle)
        while (at >= 0) {
            if (best < 0 || Math.abs(at - hint) < Math.abs(best - hint)) best = at
            at = text.indexOf(needle, at + 1)
        }
        return best
    }
}
