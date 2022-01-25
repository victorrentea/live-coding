package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.FrameworkDetector
import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.PsiModifier.FINAL
import com.intellij.psi.PsiModifier.STATIC
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PsiErrorElementUtil
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import kotlin.math.min


class AddRequiredArgsConstructorInspection : LombokJavaInspectionBase() {
    companion object {
        const val INSPECTION_NAME = "Final fields can be injected via @RequiredArgsConstructor"
        const val FIX_NAME = "Add @RequiredArgsConstructor (lombok)"
        val log = logger<AddRequiredArgsConstructorInspection>()
    }

    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME

    override fun buildVisitor(): BaseInspectionVisitor {
        return AddRequiredArgsConstructorVisitor()
    }
    class AddRequiredArgsConstructorVisitor() : BaseInspectionVisitor() {
        override fun visitElement(field: PsiElement) {
            super.visitElement(field)
            if (field !is PsiField) return
            if (field.hasModifierProperty(STATIC)) return
            if (!field.hasModifierProperty(FINAL)) return
            if (field.hasInitializer()) return // KO for private final int x = 2
            val psiClass = field.containingClass ?: return
            if (psiClass.constructors.isNotEmpty()) return

            registerError(field)
        }
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix {
        return AddRequiredArgsConstructorFix()
    }
    class AddRequiredArgsConstructorFix : InspectionGadgetsFix() {
        override fun getFamilyName() = FIX_NAME

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            val psiClass = PsiTreeUtil.getParentOfType(descriptor?.psiElement, PsiClass::class.java) ?: return
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

