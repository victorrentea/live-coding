package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.extracthints.MethodRenderer
import com.github.victorrentea.livecoding.extracthints.ExtractSuggestionRenderer
import com.github.victorrentea.livecoding.extracthints.SyntaxExtractableSectionsVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.psi.*
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.PsiErrorElementUtil

private val log = logger<SuggestMethodsToExtractVisitor>()

const val LAYER = HighlighterLayer.LAST - 1

class ExtractAssistantInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "X"
    }

    override fun inspectionStarted(session: LocalInspectionToolSession, isOnTheFly: Boolean) {
        ApplicationManager.getApplication().invokeLater {
            PsiEditorUtil.findEditor(session.file)?.let { editor ->
                println("Cleanup old markup")
                editor.markupModel.allHighlighters
                    .filter { it.layer == LAYER }
                    .forEach { editor.markupModel.removeHighlighter(it) }
            }
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return SuggestMethodsToExtractVisitor()
    }
}

enum class LocalAccess {
    READ, WRITE
}

data class LocalUsage(val lineNumber: Int, val variable: PsiVariable, val access: LocalAccess) {
    override fun toString() = "$lineNumber:" +
            (if (access == LocalAccess.READ) "R" else "W") +
            "(" + variable.name + ")"
}

class SuggestMethodsToExtractVisitor : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        val method = element as? PsiMethod ?: return
        if (PsiErrorElementUtil.hasErrors(element.project, element.containingFile.virtualFile)) {
            return
        }
        val methodBody = method.body ?: return


        val complexityVisitor = CognitiveComplexityVisitor()
        val totalComplexity = complexityVisitor.visitElement(method, 0).total()
        println("Method complexity : $totalComplexity")

        ApplicationManager.getApplication().invokeLater {
            PsiEditorUtil.findEditor(element)?.markupModel?.let { markupModel ->
                val h: RangeHighlighter = markupModel.addRangeHighlighter(
                    null,
                    method.startOffset,
                    methodBody.startOffset + 1,
                    HighlighterLayer.LAST -1,
                    HighlighterTargetArea.LINES_IN_RANGE
                )

                h.customRenderer = MethodRenderer(totalComplexity)
            }
        }

        val parameters = method.parameterList.parameters.toList()
        val variables = PsiTreeUtil.findChildrenOfType(method, PsiLocalVariable::class.java)
        log.debug("Params: $parameters")
        log.debug("Vars: $variables")

        val usages = (variables + parameters)
            .flatMap { variable -> variable.referencesToMe.flatMap { createUsages(it, variable) } } +
                variables.filter { it.hasInitializer() }
                    .map { LocalUsage(it.getLineNumber(), it, LocalAccess.WRITE) }

        val usagesMap = usages.groupBy { it.lineNumber }
            .toSortedMap()


        val sectionsExtractor = SyntaxExtractableSectionsVisitor()
        sectionsExtractor.visitElement(method)
        val allExtractableSyntaxSections = sectionsExtractor.getSections()
        println("Syntax sections: " + allExtractableSyntaxSections)

        println("Usages: $usagesMap")
        val extractOptions = mutableListOf<ExtractOption>()

        for (section in allExtractableSyntaxSections) {
            val startLine = section.first().startLineNumber()
            val endLine = section.last().endLineNumber()
            val usagesInside = (startLine..endLine).flatMap { usagesMap[it] ?: emptyList() }
            val firstUsage = usagesInside.groupBy { it.variable }
                .mapValues { (_, usage) -> usage.minByOrNull { it.lineNumber }!! }
            val inputVariables = firstUsage.filter { (_, usage) -> usage.access == LocalAccess.READ }

            val assignedVars = usagesInside.filter { it.access == LocalAccess.WRITE }
                .map { it.variable }

            val varsReadAfter = (endLine + 1 until method.endLineNumber())
                .flatMap { usagesMap[it] ?: emptyList() }
                .filter { it.access == LocalAccess.READ }
                .map { it.variable }

            val returnedVars = assignedVars.intersect(varsReadAfter.toSet())

            if (returnedVars.size >= 2) {
                log.debug("Extracting $startLine..$endLine - IMPOSSIBLE, as it returns more than 1 variable: " + returnedVars.map { it.name })
                continue
            }

//            println(section)
            val complexity = section.mapNotNull { complexityVisitor.complexityMap[it] }
                .fold(CognitiveComplexityInContext.ZERO) { acc, cc -> acc + cc }

            val hostMethodComplexityAfter = totalComplexity - complexity.costInContext
            if (hostMethodComplexityAfter <= 1) {
                log.debug("Extracting $startLine..$endLine - TOO BIG, as it leaves the host method to only $hostMethodComplexityAfter (by extracting a method of complexity ${complexity.costIfExtracted}")
                continue
            }

            if (complexity.costInContext == 0) {
                log.debug("Extracting $startLine..$endLine - TOO BOILERPLATE, of complexity: ${complexity.costInContext}")
                continue
            }

            println(
                "Extracting $startLine..$endLine takes: ${inputVariables.values.map { it.variable.name }} " +
                        "and returns ${returnedVars.map { it.name }}, " +
                        "cost=${complexity.costInContext} / if extracted=${complexity.costIfExtracted}"
            )

            val depth = PsiTreeUtil.getDepth(section.first(), method)

            extractOptions += ExtractOption(startLine to endLine, section, inputVariables.size, complexity, depth)
        }

        println("======advanced filtering=======")
        // R1: remove section if smaller than another one AND complexity == AND takes MORE parameters
        val smallerButWithMoreParams = extractOptions.filter { toRemove ->
            extractOptions.any {
                it != toRemove &&
                        it.section.containsAll(toRemove.section) &&
                        it.parameterCount < toRemove.parameterCount &&
                        it.complexity.costInContext == toRemove.complexity.costInContext
            }
        }
        println("REMOVING ${smallerButWithMoreParams.size} overlapping with more params:")
        smallerButWithMoreParams.forEach { println(it) }
        extractOptions -= smallerButWithMoreParams


        // R2: remove section if another one overlapping AND same complexity AND param count BUT larger size > aim for higher 'complexity density'
        val largerButNotMoreComplex = extractOptions.filter { toRemove ->
            extractOptions.any {
                it != toRemove &&
                        toRemove.section.containsAll(it.section) &&
                        toRemove.parameterCount == it.parameterCount &&
                        toRemove.complexity.costInContext == it.complexity.costInContext
            }
        }
        println("REMOVING ${largerButNotMoreComplex.size} larger but not more complex:")
        largerButNotMoreComplex.forEach { println(it) }
        extractOptions -= largerButNotMoreComplex

        assignDisplayDepth(extractOptions)

        extractOptions.sortByDescending { it.complexity.costInContext }

        println("=== final results ===")

        ApplicationManager.getApplication().invokeLater {
            extractOptions.forEach { extract ->
                println(extract)
                PsiEditorUtil.findEditor(element)?.markupModel?.let { markupModel ->
                    val h: RangeHighlighter = markupModel.addRangeHighlighter(
                        null,
                        extract.section.first().startOffset,
                        extract.section.last().endOffset,
                        LAYER,
                        HighlighterTargetArea.LINES_IN_RANGE
                    )

                    h.customRenderer = ExtractSuggestionRenderer(extract.displayHanging)
                }
            }
        }
    }

    private fun assignDisplayDepth(extractOptions: MutableList<ExtractOption>) {
        println("Start assign depth to " + extractOptions.map { it.lines })
        var found = false
        for (i in 1 .. 30) {
            // TODO test caused infinite loop once: Start assign depth to [(60, 75), (64, 65), (65, 65), (79, 80), (80, 80)]
            for (extract in extractOptions) {
                extractOptions.filter {
                    it != extract &&
                    it.depth == extract.depth &&
                    extract.displayHanging < it.displayHanging + 1 &&
                     it.intersects(extract) &&
                    (it.startLine > extract.startLine // first to start is more to right
                        || extract.lineCount > it.lineCount)  // larger is on the left
                }.forEach {
                    println("INC depth of "+extract.lines + " to " + (it.displayHanging + 1) + " because of " + it.lines)
                    extract.displayHanging = it.displayHanging + 1
                    found = true
                }
            }
            if (!found) break
        }
        println("Done assign depts: " + extractOptions.map { it.lines to it.displayHanging })
    }

    private fun createUsages(
        expression: PsiReferenceExpression,
        variable: PsiVariable
    ): List<LocalUsage> {
        val result = mutableListOf<LocalUsage>()
        if (expression.isRead()) result += LocalUsage(expression.getLineNumber(), variable, LocalAccess.READ)
        if (expression.isWrite()) result += LocalUsage(expression.getLineNumber(), variable, LocalAccess.WRITE)
        return result
    }
}


data class ExtractOption(
    val lines: Pair<Int, Int>,
    val section: List<PsiStatement>,
    val parameterCount: Int,
    val complexity: CognitiveComplexityInContext,
    val depth: Int,
    var displayHanging: Int = 0
) {
    fun containedWithin(other: ExtractOption): Boolean =
        startLine >= other.startLine && endLine <= other.endLine

    fun intersects(other: ExtractOption): Boolean =
        startLine <= other.endLine && other.startLine <= endLine

    val lineCount get() = endLine - startLine + 1
    val startLine get() = lines.first
    val endLine get() = lines.second
}