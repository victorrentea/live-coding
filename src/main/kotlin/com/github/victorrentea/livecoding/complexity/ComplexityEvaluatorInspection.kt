//package com.github.victorrentea.livecoding.complexity
//
//import com.intellij.openapi.diagnostic.logger
//import com.intellij.psi.JavaRecursiveElementWalkingVisitor
//import com.intellij.psi.PsiJavaFile
//import com.intellij.psi.PsiMethod
//import com.intellij.refactoring.suggested.startOffset
//import com.siyeh.ig.BaseInspection
//import com.siyeh.ig.BaseInspectionVisitor
//
//class ComplexityEvaluatorInspection : BaseInspection() {
//    companion object {
//        private val log = logger<ComplexityEvaluatorInspection>()
//    }
//
//    override fun buildErrorString(vararg infos: Any?) = "Inspecting method complexity"
//
//    override fun buildVisitor() = ComplexityVisitor()
//
//    class ComplexityVisitor : BaseInspectionVisitor() {
//        override fun visitJavaFile(file: PsiJavaFile?) {
//            val visitor = RecursiveComplexityVisitor()
//            visitor.visitJavaFile(file)
//            val complexitiesToDisplay = visitor.complexityByMethod
//                .map { extractForDisplay(it.key, it.value) }.filterNotNull()
//
//            val project = file?.project ?: return
//            project.messageBus.syncPublisher(RenderComplexityNotifier.CHANGE_ACTION_TOPIC)
//                .complexityComputed(file.virtualFile.url, complexitiesToDisplay)
//        }
//        private fun extractForDisplay(method: PsiMethod?, complexity: Int): ComplexityToDisplayInFile? {
//            val methodBody = method?.body ?: return null
//            return ComplexityToDisplayInFile(methodBody.startOffset + 1, complexity)
//        }
//    }
//    class RecursiveComplexityVisitor : JavaRecursiveElementWalkingVisitor() {
//        val complexityByMethod = mutableMapOf<PsiMethod, Int>()
//        override fun visitMethod(method: PsiMethod?) {
//            if (method == null) return
//            val complexity = CognitiveComplexityVisitor().visitElement(method).total()
//            complexityByMethod[method] = complexity
//        }
//    }
//}
//
