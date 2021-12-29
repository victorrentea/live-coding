package com.github.victorrentea.livecoding

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

private val log = logger<SuggestMethodsToExtractVisitor>()


//class AnalyzeCognitiveComplexityAction : AnAction() {
//    override fun actionPerformed(e: AnActionEvent) {
////        val psiFile = e.getData(CommonDataKeys.PSI_ELEMENT)?.containingFile
//        val psiFile = e.getData(CommonDataKeys.PSI_ELEMENT)
//            ?.let { PsiTreeUtil.getParentOfType(it, PsiMethod::class.java) }
////        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
//        println("File:  $psiFile")
//        val visitor = CognitiveComplexityVisitor()
//        psiFile?.accept(visitor)
//
////        val editor = e.getData(CommonDataKeys.EDITOR)
////        ApplicationManager.getApplication().invokeLater {
////            visitor.report()?.let { reportString ->
////                editor?.let { editor ->
////                    HintManager.getInstance().showInformationHint(editor, reportString)
////                }
////            }
////        }
//    }
//}
class CCInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "X"
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

        val complexityVisitor = CognitiveComplexityVisitor()
        val totalComplexity = complexityVisitor.visitElement(method, 0).total()
        println("Method complexity : $totalComplexity")

        val parameters = method.parameterList.parameters.toList()
        val variables = PsiTreeUtil.findChildrenOfType(method, PsiLocalVariable::class.java)
        log.debug("Params: $parameters")
        log.debug("Vars: $variables")

        val usages = (variables + parameters)
            .flatMap { variable -> variable.referencesToMe.flatMap {createUsages(it, variable)}} +
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
            val complexity = section.mapNotNull{complexityVisitor.complexityMap[it]}
                .fold(CognitiveComplexityInContext.ZERO) {acc, cc -> acc + cc}

            val hostMethodComplexityAfter = totalComplexity - complexity.costInContext
            if (hostMethodComplexityAfter <= 1) {
                log.debug("Extracting $startLine..$endLine - TOO BIG, as it leaves the host method to only $hostMethodComplexityAfter (by extracting a method of complexity ${complexity.costIfExtracted}")
                continue
            }

            if (complexity.costInContext == 0) {
                log.debug("Extracting $startLine..$endLine - TOO BOILERPLATE, of complexity: ${complexity.costInContext}")
                continue
            }

            println("Extracting $startLine..$endLine takes: ${inputVariables.values.map { it.variable.name }} " +
                    "and returns ${returnedVars.map { it.name }}, " +
                    "cost=${complexity.costInContext} / if extracted=${complexity.costIfExtracted}")

            extractOptions += ExtractOption(startLine to endLine,section, inputVariables.size, complexity)
        }

        println("======advanced filtering=======")
        // R1: remove section if smaller than another one AND complexity == AND takes MORE parameters
        val smallerButWithMoreParams = extractOptions.filter { toRemove ->
            extractOptions.any {
                it != toRemove &&
                it.section.containsAll(toRemove.section) &&
                it.parameterCount < toRemove.parameterCount &&
                it.complexity.costInContext == toRemove.complexity.costInContext } }
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

        extractOptions.sortByDescending { it.complexity.costInContext }
        println("=== final results ===")

        extractOptions.forEach { println(it) }
    }

    private fun createUsages(
        expression: PsiReferenceExpression,
        variable: PsiVariable
    ):List<LocalUsage> {
        val result = mutableListOf<LocalUsage>()
        if (expression.isRead()) result += LocalUsage(expression.getLineNumber(), variable, LocalAccess.READ)
        if (expression.isWrite()) result += LocalUsage(expression.getLineNumber(), variable, LocalAccess.WRITE)
        return result
    }
}


data class ExtractOption(val lines: Pair<Int,Int>, val section: List<PsiStatement>, val parameterCount:Int, val complexity:CognitiveComplexityInContext)