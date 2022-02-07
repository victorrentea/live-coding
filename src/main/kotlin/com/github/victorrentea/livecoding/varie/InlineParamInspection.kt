//package com.github.victorrentea.livecoding.varie
//
//import com.intellij.psi.PsiMethod
//import com.intellij.psi.PsiMethodCallExpression
//import com.intellij.refactoring.inline.InlineParameterExpressionProcessor
//import com.siyeh.ig.BaseInspection
//import com.siyeh.ig.BaseInspectionVisitor
//
//class InlineParamInspection : BaseInspection() {
//    companion object {
//        const val INSPECTION_NAME = "Parameter can be inlined"
//        const val FIX_NAME = "Inline Parameter"
//    }
//
//    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME
//
//    override fun buildVisitor() = InlineParamVisitor()
//
//    class InlineParamVisitor : BaseInspectionVisitor() {
//        override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
//            val calledMethod = expression?.methodExpression?.resolve() as? PsiMethod ?: return
//
////            calledMethod.
//                    InlineParameterExpressionProcessor
//        }
//    }
//}