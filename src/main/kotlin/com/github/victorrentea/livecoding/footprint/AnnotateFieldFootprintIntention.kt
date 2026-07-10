package com.github.victorrentea.livecoding.footprint

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil

/**
 * On-demand (Alt+Enter) intention: runs the transitive [FieldFootprintAnalyzer] on each
 * fat-object parameter of the enclosing method and writes/updates a
 * `@param <name> reads {a, b, c} writes {d}` tag in the Javadoc.
 *
 * On-demand by design: the transitive analysis is too expensive for on-the-fly inspection, so
 * it runs behind an explicit action + progress bar. The written annotation then acts as the
 * persisted cache a cheap live inspection can watch.
 */
class AnnotateFieldFootprintIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = "Annotate parameter field footprint"

    override fun getText(): String = "Annotate field footprint in Javadoc"

    override fun startInWriteAction(): Boolean = false

    // The default preview would run this (expensive) intention on a copy -> disable it.
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false) ?: return false
        return method.parameterList.parameters.any { FootprintTargets.isTarget(it.type) }
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false) ?: return
        val params = method.parameterList.parameters.filter { FootprintTargets.isTarget(it.type) }
        if (params.isEmpty()) return

        // Heavy transitive analysis: background thread + read action + cancelable progress.
        val compute = ThrowableComputable<List<Pair<String, String>>, RuntimeException> {
            ReadAction.compute<List<Pair<String, String>>, RuntimeException> {
                params.mapNotNull { p ->
                    val fqn = PsiTypesUtil.getPsiClass(p.type)?.qualifiedName ?: return@mapNotNull null
                    p.name to clauseFor(FieldFootprintAnalyzer(fqn).analyzeParameter(method, p))
                }
            }
        }
        val clauses: List<Pair<String, String>> =
            if (ApplicationManager.getApplication().isUnitTestMode) {
                compute.compute()
            } else {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    compute, "Analyzing Field Footprint", true, project,
                )
            }

        WriteCommandAction.runWriteCommandAction(
            project, getText(), null,
            Runnable { writeAnnotations(project, method, clauses) },
            method.containingFile, // prepare this file for write (else: read-only modification error)
        )
    }

    private fun clauseFor(fp: Footprint): String {
        val reads = when (fp.verdict) {
            Verdict.SOUND -> if (fp.paths.isEmpty()) "none" else fp.paths.sorted().joinToString(", ")
            Verdict.WHOLE_OBJECT -> "ALL"
            Verdict.UNKNOWN -> "?"
        }
        return buildString {
            append("reads {").append(reads).append('}')
            if (fp.writes.isNotEmpty()) append(" writes {").append(fp.writes.sorted().joinToString(", ")).append('}')
            if (fp.verdict != Verdict.SOUND) append(" — ").append(fp.reasons.firstOrNull().orEmpty())
        }
    }

    private fun writeAnnotations(project: Project, method: PsiMethod, clauses: List<Pair<String, String>>) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val doc = method.docComment
        if (doc == null) {
            val text = buildString {
                append("/**\n")
                for ((name, clause) in clauses) append(" * @param ").append(name).append(' ').append(clause).append('\n')
                append(" */")
            }
            method.addBefore(factory.createDocCommentFromText(text), method.firstChild)
        } else {
            for ((name, clause) in clauses) upsertParamTag(factory, doc, name, clause)
        }
        // Reformat only the Javadoc range (a PsiDocComment isn't a valid standalone format root).
        val doc2 = method.docComment ?: return
        CodeStyleManager.getInstance(project)
            .reformatRange(method, doc2.textRange.startOffset, doc2.textRange.endOffset)
    }

    private fun upsertParamTag(factory: PsiElementFactory, doc: PsiDocComment, paramName: String, clause: String) {
        val existing = doc.tags.firstOrNull { it.name == "param" && it.valueElement?.text == paramName }
        // Preserve any hand-written description, minus a previous reads/writes block.
        val description = existing?.dataElements?.drop(1)?.joinToString(" ") { it.text }
            ?.let { CLAUSE.replace(it, "") }?.trim().orEmpty()
        val tagText = buildString {
            append("@param ").append(paramName).append(' ')
            if (description.isNotEmpty()) append(description).append(' ')
            append(clause)
        }
        val newTag = factory.createDocTagFromText(tagText)
        if (existing != null) existing.replace(newTag) else doc.add(newTag)
    }

    companion object {
        private val CLAUSE = Regex("""reads\s*\{[^}]*}(\s*writes\s*\{[^}]*})?(\s*—[^\n\r*]*)?""")
    }
}
