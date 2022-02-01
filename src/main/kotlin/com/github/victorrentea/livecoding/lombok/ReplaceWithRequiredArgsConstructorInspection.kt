package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.checkItemsAre
import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix


class ReplaceWithRequiredArgsConstructorInspection : AbstractLombokJavaInspectionBase() {
    companion object {
        val log = logger<ReplaceWithRequiredArgsConstructorInspection>()
        const val INSPECTION_NAME = "Boilerplate constructor only injecting dependencies"
        const val FIX_NAME = "Replace with @RequiredArgsConstructor (lombok)"
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return ReplaceRequiredArgsConstructorVisitor()
    }
    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix {
        return ReplaceRequiredArgsConstructorFix()
    }

    class ReplaceRequiredArgsConstructorVisitor : BaseInspectionVisitor() {
        override fun visitElement(constructor: PsiElement) {
            super.visitElement(constructor)
            if (constructor !is PsiMethod) return
            if (!constructor.isConstructor) return

            if (!constructorOnlyCopiesParamsToFinalFields(constructor)) return

            if (constructor.parameterList.parametersCount >= 2 && isSpringBean(constructor.containingClass)) {
                registerMethodError(constructor)
            } else {
                registerError(constructor, ProblemHighlightType.INFORMATION)
            }
        }

        private fun constructorOnlyCopiesParamsToFinalFields(constructor: PsiMethod): Boolean {
            val body = constructor.body ?: return false

            val assignments = body.statements.map {it.firstChild}.toList().checkItemsAre<PsiAssignmentExpression>()
                ?: return false // non assignments statements found

            val containingClass = constructor.containingClass ?: return false

            val finalFields = containingClass.fields
                .filter {it.hasModifierProperty(PsiModifier.FINAL) }
                .filter {!it.hasModifierProperty(PsiModifier.STATIC)  }

            val constructorParams = constructor.parameterList.parameters

            for (assignment in assignments) {
                // a field is assigned
                val assignedExpr = assignment.lExpression as? PsiReferenceExpression ?: return false
                val assignedField = assignedExpr.resolve() as? PsiField ?: return false

                // assigned to a parameter
                val valueExpr = assignment.rExpression as? PsiReferenceExpression ?: return false
                val valueParam = valueExpr.resolve() as? PsiParameter ?: return false

                val paramIndex = constructorParams.indexOf(valueParam)
                val fieldIndex = finalFields.indexOf(assignedField)
                if (paramIndex != fieldIndex) return false // incorrect order
            }
            return true
        }

        private fun isSpringBean(psiClass: PsiClass?): Boolean  {
            if (psiClass?.hasAnnotation("org.springframework.stereotype.Component") == true) {
                return true
            }
            fun isComponentStereotype(annotation: PsiAnnotation) =
                annotation.resolveAnnotationType()?.hasAnnotation("org.springframework.stereotype.Component") == true
            if (psiClass?.annotations?.any {isComponentStereotype(it)} == true) {
                return true
            }
            return false
        }
    }

    class ReplaceRequiredArgsConstructorFix : InspectionGadgetsFix() {
        override fun getFamilyName() = FIX_NAME

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            val constructor = PsiTreeUtil.getParentOfType(descriptor?.psiElement, PsiMethod::class.java) ?: return
            val parentClass = PsiTreeUtil.getParentOfType(constructor, PsiClass::class.java) ?: return
            val modifiers = parentClass.modifierList ?: return

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project, FIX_NAME, "Live-Coding", {
                    val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)

                    constructor.delete()
                })
            }
        }

    }
}



