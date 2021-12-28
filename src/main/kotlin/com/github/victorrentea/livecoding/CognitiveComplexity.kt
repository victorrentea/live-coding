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

        val visitor = CognitiveComplexityVisitor()
        visitor.visitElement(method)
        println("Method complexity : " + visitor.complexity)

        val parameters = method.parameterList.parameters.toList()
        val variables = PsiTreeUtil.findChildrenOfType(method, PsiLocalVariable::class.java)
        log.debug("Params: $parameters")
        log.debug("Vars: $variables")

        val usages = (variables + parameters)
            .flatMap { variable ->
                variable.referencesToMe.map {
                    LocalUsage(
                        it.getLineNumber(),
                        variable,
                        if (it.isRead()) LocalAccess.READ else LocalAccess.WRITE
                    )
                }
            } + variables.filter { it.hasInitializer() }
            .map { LocalUsage(it.getLineNumber(), it, LocalAccess.WRITE) }

        val usagesMap = usages.groupBy { it.lineNumber }
            .toSortedMap()


        val sectionsExtractor = SyntaxExtractableSectionsVisitor()
        sectionsExtractor.visitElement(method)
        val allExtractableSyntaxSections = sectionsExtractor.getSections()
        println("Syntax sections: " + allExtractableSyntaxSections)

        println("Usages: $usagesMap")

//        for (i in usagesMap.keys.indices)
//            for (j in i + 1 until usagesMap.keys.size) {
        for (section in allExtractableSyntaxSections) {
                val startLine = section.first
                val endLine = section.second
//                val lines = (startLine..endLine).map { usagesMap.keys.toList()[it] }
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
                    log.debug("Impossible to extract $section as it returns more than 1 variable: " + inputVariables.values.map { it.variable.name })
                    continue
                }


                println("Extracting $section takes: ${inputVariables.values.map { it.variable.name }} and returns ${returnedVars.map { it.name }}")
            }


    }
}
