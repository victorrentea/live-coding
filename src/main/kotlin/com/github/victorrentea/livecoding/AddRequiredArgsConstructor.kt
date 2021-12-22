package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.AddRequiredArgsConstructorInspection.Companion.INSPECTION_NAME
import com.github.victorrentea.livecoding.FrameworkDetector.lombokIsPresent
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.PsiModifier.FINAL
import com.intellij.psi.PsiModifier.STATIC
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.min


class AddRequiredArgsConstructorInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Final fields can be injected via @RequiredArgsConstructor"
    }
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!lombokIsPresent(holder.file))
            return PsiElementVisitor.EMPTY_VISITOR

        return AddRequiredArgsConstructorVisitor(holder)
    }
}

class AddRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(field: PsiElement) {
        super.visitElement(field)
        if (field !is PsiField) return
        if (field.hasModifierProperty(STATIC)) return
        if (!field.hasModifierProperty(FINAL)) return
        if (field.hasInitializer()) return // private final int x = 2

        val noOfConstructors = field.containingClass?.constructors?.size ?: return
        if (noOfConstructors >= 2) return

        val existingConstructor = if (noOfConstructors == 1) field.containingClass?.constructors!![0]!! else null
        if (existingConstructor != null && !constructorOnlyCopiesParamsToFields(existingConstructor)) return

        val textLength = min(
            field.nameIdentifier.textRangeInParent.endOffset + 1,
            field.nameIdentifier.parent.textRange.length
        )
        val textRange = TextRange(0, textLength)  // +1 so ALT-ENTER works even after ;

        if (!shouldRegisterError(field, existingConstructor)) return

        holder.registerProblem(
            field,
            INSPECTION_NAME,
            ProblemHighlightType.GENERIC_ERROR, // red underline
            textRange,
            AddRequiredArgsConstructorFix(field, existingConstructor)
        )
    }

    private fun shouldRegisterError(field: PsiField, constructor: PsiMethod?): Boolean {
        if (constructor == null) return true
        if (!constructor.parameterList.parameters.map { it.name }.contains(field.name)) return true;
        return false
    }
}

class AddRequiredArgsConstructorFix(field: PsiField, constructor: PsiMethod?) : LocalQuickFixOnPsiElement(field, constructor) {
    companion object {
        const val FIX_NAME = "Add @RequiredArgsConstructor (lombok)"
    }
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = FIX_NAME

    override fun invoke(project: Project, file: PsiFile, field: PsiElement, constructor: PsiElement) {
        val parentClass = PsiTreeUtil.getParentOfType(startElement, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return
        WriteCommandAction.runWriteCommandAction(project, FIX_NAME, "Live-Coding", {
            val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
            constructor.delete()
        })
    }

}