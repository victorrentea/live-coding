package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.lombok.AddRequiredArgsConstructorInspection.Companion.INSPECTION_NAME
import com.github.victorrentea.livecoding.FrameworkDetector.lombokIsPresent
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.PsiModifier.FINAL
import com.intellij.psi.PsiModifier.STATIC
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import kotlin.math.min


class AddRequiredArgsConstructorInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Final fields can be injected via @RequiredArgsConstructor"
        const val FIX_NAME = "Add @RequiredArgsConstructor (lombok)"
    }
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!lombokIsPresent(holder.file)) {
//            println("NO LOMBOK")
            return PsiElementVisitor.EMPTY_VISITOR
        }
        return AddRequiredArgsConstructorVisitor(holder)
    }

    class AddRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        override fun visitElement(field: PsiElement) {
            super.visitElement(field)
            if (field !is PsiField) return
            if (field.hasModifierProperty(STATIC)) return
            if (!field.hasModifierProperty(FINAL)) return
            if (field.hasInitializer()) return // KO for private final int x = 2
            val psiClass = field.containingClass ?: return
            if (psiClass.constructors.size >= 2) return

            val existingConstructor = psiClass.constructors.getOrNull(0)
            if (existingConstructor != null) {
                if (!constructorOnlyCopiesParamsToFields(existingConstructor)) return
                if (existingConstructor.parameterList.parameters.map{ it.name }.contains(field.name)) return
            }

            val textLength = min(
                field.nameIdentifier.textRangeInParent.endOffset + 1,
                field.nameIdentifier.parent.textRange.length
            )
            val textRange = TextRange(0, textLength)  // +1 so ALT-ENTER works even after ;

            holder.registerProblem(
                field,
                INSPECTION_NAME,
                ProblemHighlightType.GENERIC_ERROR, // red underline
                textRange,
                AddRequiredArgsConstructorFix(psiClass)
            )
        }

    }

    class AddRequiredArgsConstructorFix(psiClass: PsiClass) : LocalQuickFixOnPsiElement(psiClass) {
        override fun getFamilyName() = "Live-Coding"

        override fun getText() = FIX_NAME

        override fun invoke(project: Project, file: PsiFile, psiClass: PsiElement, same: PsiElement) {
            if (psiClass !is PsiClass) return
            val modifiers = psiClass.modifierList ?: return
            val constructor = psiClass.constructors.getOrNull(0)
            WriteCommandAction.runWriteCommandAction(project, FIX_NAME, "Live-Coding", {
                val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
                constructor?.delete()
            })
        }

    }
}

