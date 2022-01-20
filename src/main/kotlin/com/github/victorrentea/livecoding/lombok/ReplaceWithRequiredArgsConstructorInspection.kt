package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.FrameworkDetector
import com.github.victorrentea.livecoding.checkItemsAre
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil


class ReplaceWithRequiredArgsConstructorInspection : LocalInspectionTool() {
    companion object {
        val log = logger<ReplaceWithRequiredArgsConstructorInspection>()
        const val INSPECTION_NAME = "Boilerplate constructor only injecting dependencies"
        const val FIX_NAME = "Replace with @RequiredArgsConstructor (lombok)"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!FrameworkDetector.lombokIsPresent(holder.file)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        return ReplaceRequiredArgsConstructorVisitor(holder)
    }

    class ReplaceRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        override fun visitElement(constructor: PsiElement) {
            super.visitElement(constructor)
            if (constructor !is PsiMethod) return
            if (!constructor.isConstructor) return

            if (!constructorOnlyCopiesParamsToFinalFields(constructor)) return

            if (constructor.parameterList.parametersCount >= 2 && isSpringBean(constructor.containingClass)) {
                val constructorName = PsiTreeUtil.findChildOfType(constructor, PsiIdentifier::class.java) ?: constructor

                holder.registerProblem(
                    constructorName,
                    INSPECTION_NAME,
                    ProblemHighlightType.WEAK_WARNING,
                    ReplaceRequiredArgsConstructorFix(constructor))
            } else {
                holder.registerProblem(
                    constructor,
                    INSPECTION_NAME,
                    ProblemHighlightType.INFORMATION,
                    ReplaceRequiredArgsConstructorFix(constructor))
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

    class ReplaceRequiredArgsConstructorFix(constructor: PsiMethod) : LocalQuickFixOnPsiElement(constructor) {
        override fun getFamilyName() = "Live-Coding"

        override fun getText() = FIX_NAME

        override fun invoke(project: Project, file: PsiFile, constructor: PsiElement, endElement: PsiElement) {
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



