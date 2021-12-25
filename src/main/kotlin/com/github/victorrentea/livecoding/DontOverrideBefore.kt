package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.ReplaceRequiredArgsConstructorInspection.Companion.INSPECTION_NAME
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.OverridingMethodsSearch


class DontOverrideBeforeInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Dangerous override of inherited before method.\nIt is a bad practice to override before methods and erase their behavior."
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DontOverrideBeforeVisitor(holder)
    }
}

class DontOverrideBeforeVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        val method = element as? PsiMethod ?: return

        if (!method.modifierList.hasAnyAnnotation("org.junit.jupiter.api.BeforeEach", "org.junit.Before")) return

        if (method.findSuperMethods().isEmpty()) return

        holder.registerProblem(
            method.nameIdentifier!!,
            DontOverrideBeforeInspection.INSPECTION_NAME,
            ProblemHighlightType.WARNING
        )
    }
}

