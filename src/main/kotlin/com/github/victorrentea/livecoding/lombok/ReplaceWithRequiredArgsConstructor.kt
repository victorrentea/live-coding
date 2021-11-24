package com.github.victorrentea.livecoding.lombok

import com.intellij.codeInspection.*
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil


class ReplaceWithRequiredArgsConstructorInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!LombokUtil.lombokIsPresent(holder.project, holder.file))
            return PsiElementVisitor.EMPTY_VISITOR

        return ReplaceWithRequiredArgsConstructorVisitor(holder)
    }
}
class ReplaceWithRequiredArgsConstructorVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitElement(constructor: PsiElement) {
        if (constructor !is PsiMethod || !constructor.isConstructor)
            return
        val body = constructor.body ?: return

        val statements = body.children.filterIsInstance<PsiExpressionStatement>()
        if (!statements.all { it.firstChild is PsiAssignmentExpression }) {
            return // Found non assignments in constructor
        }
        val assignments = PsiTreeUtil.collectElementsOfType(constructor, PsiAssignmentExpression::class.java)

        val finalFields = constructor.containingClass!!.fields
            .filter {it.hasModifierProperty(PsiModifier.FINAL) }
            .filter {!it.hasModifierProperty(PsiModifier.STATIC)  }
            .map { it.name }

        val constructorParams = constructor.parameterList.parameters.map { it.name }
        log.debug("Constructor parameters: $constructorParams, final fields: $finalFields")

        if (finalFields.size != constructorParams.size) return // not same number of final fields as params


        if (!assignments.all {
            it.firstChild is PsiReferenceExpression &&
            it.firstChild.firstChild is PsiThisExpression
        }) return // Some assignments don't assign to fields

        val fieldToParam = assignments.map { (it.firstChild.lastChild.text to it.lastChild.text) }
        log.debug("Field = param: $fieldToParam")

        if (!fieldToParam.all { finalFields.indexOf(it.first) == constructorParams.indexOf(it.second) }) {
            log.debug("Fields and Params are in different order")
            return
        }

        holder.registerProblem(constructor,
            "Constructor can be replaced with @RequiredArgsConstructor",
            ProblemHighlightType.WEAK_WARNING,
            ReplaceWithRequiredArgsConstructorQuickFix(constructor))
    }

    companion object {
        val log = Logger.getInstance(ReplaceWithRequiredArgsConstructorVisitor::class.java)
    }
}

class ReplaceWithRequiredArgsConstructorQuickFix(constructor: PsiMethod) : LocalQuickFixOnPsiElement(constructor) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Replace with @RequiredArgsConstructor (lombok)"

    override fun invoke(project: Project, file: PsiFile, constructor: PsiElement, endElement: PsiElement) {
        val parentClass = PsiTreeUtil.getTopmostParentOfType(constructor, PsiClass::class.java) ?: return
        val modifiers = parentClass.modifierList ?: return
        val annotation = modifiers.addAnnotation("lombok.RequiredArgsConstructor")
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)

        constructor.delete()
    }

}

