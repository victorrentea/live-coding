//package com.github.victorrentea.livecoding.lombok
//
//import com.github.victorrentea.livecoding.FrameworkDetector.lombokIsPresent
//import com.intellij.codeInspection.ProblemHighlightType
//import com.intellij.codeInspection.ProblemsHolder
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiElementVisitor
//import com.intellij.psi.PsiField
//import com.intellij.psi.PsiModifier
//import com.siyeh.ipp.base.Intention
//import com.siyeh.ipp.base.PsiElementPredicate
//import kotlin.math.min
//
//
//class AddRequiredArgsConstructorIntention : Intention() {
//    companion object {
//        const val INSPECTION_NAME = "Final fields can be injected via @RequiredArgsConstructor"
//    }
//
//    override fun getElementPredicate(): PsiElementPredicate {
//        return PsiElementPredicate { element ->
//            lombokIsPresent(element) &&
//        }
//    }
//
//    fun canAddRAC() {
//        if (field !is PsiField) return
//        if (field.hasModifierProperty(PsiModifier.STATIC)) return
//        if (!field.hasModifierProperty(PsiModifier.FINAL)) return
//        if (field.hasInitializer()) return // private final int x = 2
//
//        val noOfConstructors = field.containingClass?.constructors?.size ?: return
//        if (noOfConstructors >= 2) return
//
//        val existingConstructor = if (noOfConstructors == 1) field.containingClass?.constructors!![0]!! else null
//        if (existingConstructor != null && !constructorOnlyCopiesParamsToFields(existingConstructor)) return
//
//        val textLength = min(
//            field.nameIdentifier.textRangeInParent.endOffset + 1,
//            field.nameIdentifier.parent.textRange.length
//        )
//        val textRange = TextRange(0, textLength)  // +1 so ALT-ENTER works even after ;
//
//        if (!shouldRegisterError(field, existingConstructor)) return
//
//        holder.registerProblem(
//            field,
//            AddRequiredArgsConstructorInspection.INSPECTION_NAME,
//            ProblemHighlightType.GENERIC_ERROR, // red underline
//            textRange,
//            AddRequiredArgsConstructorFix(field, existingConstructor)
//        )
//    }
//
//    override fun processIntention(element: PsiElement) {
//        TODO("Not yet implemented")
//    }
//
//}
