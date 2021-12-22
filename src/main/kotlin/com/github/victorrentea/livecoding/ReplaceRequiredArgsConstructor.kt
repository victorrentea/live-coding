package com.github.victorrentea.livecoding

import com.github.victorrentea.livecoding.ReplaceRequiredArgsConstructorInspection.Companion.INSPECTION_NAME
import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

private val log = logger<ReplaceRequiredArgsConstructorInspection>()

class ReplaceRequiredArgsConstructorInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Boilerplate constructor only injecting dependencies"
    }
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!FrameworkDetector.lombokIsPresent(holder.file)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return ReplaceRequiredArgsConstructorVisitor(holder)
    }
}
class ReplaceRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {

    override fun visitElement(constructor: PsiElement) {
        super.visitElement(constructor)
        if (constructor !is PsiMethod) return
        if (!constructor.isConstructor) return
        val body = constructor.body ?: return

        val statements = body.children.filterIsInstance<PsiExpressionStatement>()
        if (!statements.all { it.firstChild is PsiAssignmentExpression }) {
            return // non assignments in constructor
        }
        val assignments = PsiTreeUtil.collectElementsOfType(constructor, PsiAssignmentExpression::class.java)

        val finalFields = constructor.containingClass!!.fields
            .filter {it.hasModifierProperty(PsiModifier.FINAL) }
            .filter {!it.hasModifierProperty(PsiModifier.STATIC)  }
            .map { it.name }

        val constructorParams = constructor.parameterList.parameters.map { it.name }
//        log.debug("Constructor parameters: $constructorParams, final fields: $finalFields")

        if (finalFields.size != constructorParams.size) return // not same number of final fields as params


        if (!assignments.all {
            it.firstChild is PsiReferenceExpression &&
            it.firstChild.firstChild is PsiThisExpression
        }) return // Some assignments don't assign to fields

        val fieldToParam = assignments.map { (it.firstChild.lastChild.text to it.lastChild.text) }
//        log.debug("Field = param: $fieldToParam")

        if (!fieldToParam.all { finalFields.indexOf(it.first) == constructorParams.indexOf(it.second) }) {
//            log.debug("Fields and Params are in different order")
            return
        }

        val severity =
            if (constructor.parameterList.parametersCount >= 2 && isSpringBean(constructor))
                ProblemHighlightType.WEAK_WARNING
            else ProblemHighlightType.INFORMATION

        val constructorName = PsiTreeUtil.findChildOfType(constructor, PsiIdentifier::class.java) ?: constructor

        holder.registerProblem(constructorName,
            INSPECTION_NAME,
            severity,
            ReplaceRequiredArgsConstructorFix(constructor)
        )
    }

    private fun isSpringBean(constructor: PsiMethod):Boolean =
        constructor.containingClass?.annotations?.any { it.resolveAnnotationType()?.hasAnnotation("org.springframework.stereotype.Component")?:false } ?: false
        || constructor.containingClass?.hasAnnotation("org.springframework.stereotype.Component") ?: false
}

class ReplaceRequiredArgsConstructorFix(constructor: PsiMethod) : LocalQuickFixOnPsiElement(constructor) {
    companion object {
        const val FIX_NAME = "Replace with @RequiredArgsConstructor (lombok)"
    }
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = FIX_NAME

    override fun invoke(project: Project, file: PsiFile, constructor: PsiElement, endElement: PsiElement) {
        val parentClass = PsiTreeUtil.getParentOfType(constructor, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return

        WriteCommandAction.runWriteCommandAction(project, FIX_NAME, "Live-Coding", {
            val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)

            constructor.delete()
        })
    }

}

