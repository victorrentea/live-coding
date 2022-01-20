package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.FrameworkDetector.jpaIsPresent
import com.github.victorrentea.livecoding.FrameworkDetector.lombokIsPresent
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.addSiblingAfter
import org.jetbrains.annotations.NotNull

class LombokAndHibernateInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!lombokIsPresent(holder.file) || !jpaIsPresent(holder.file))
            return PsiElementVisitor.EMPTY_VISITOR

        return LombokAndHibernateVisitor(holder)
    }

}

class LombokAndHibernateVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {

    override fun visitElement(psiClass: PsiElement) {
        if (psiClass !is PsiClass) return
        if (!psiClass.hasAnnotation("javax.persistence.Entity")) return

        psiClass.getAnnotation("lombok.Data")?.let {
            holder.registerProblem(
                it,
                "Lombok is generating dangerous code for a JPA @Entity: toString, hashCode/equals",
                ProblemHighlightType.WEAK_WARNING,
                ReplaceDataOnEntityQuickFix(psiClass)
            )
        }

        psiClass.getAnnotation("lombok.ToString")?.let {
            // check for children
            if (psiClass.fields.any {
                    it.hasCollectionType() &&
                            !it.hasAnnotation("lombok.ToString.Exclude") &&
                            !it.hasAnnotation("lombok.ToString.Include")
                }) {
                holder.registerProblem(
                    it,
                    "Lombok is generating dangerous code for a JPA @Entity: toString",
                    ProblemHighlightType.WEAK_WARNING,
                    ExcludeCollectionsFromToStringQuickFix(psiClass)
                )
            }
        }

        psiClass.getAnnotation("lombok.EqualsAndHashCode")?.let {
            holder.registerProblem(
                it,
                "Lombok is generating dangerous code for a JPA @Entity",
                ProblemHighlightType.WEAK_WARNING,
                RemoveHashCodeEqualsQuickFix(psiClass)
            )
        }
    }
}

/** Adds the annotation if not already present */
fun PsiModifierListOwner.setAnnotation(
    qualifiedName: String,
    shortenFQName: Boolean = true
): PsiAnnotation? {
    val modifiers = modifierList ?: return null
    if (modifiers.hasAnnotation(qualifiedName)) return null
    val annotation = modifiers.addAnnotation(qualifiedName)
    if (shortenFQName) {
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
    }
    return annotation
}

fun PsiModifierListOwner.removeAnnotation(qualifiedName: String) {
    val modifiers = modifierList ?: return
    val annotation = modifiers.findAnnotation(qualifiedName) ?: return
    annotation.delete()
    JavaCodeStyleManager.getInstance(project).removeRedundantImports(containingFile as @NotNull PsiJavaFile)
}

fun PsiField.hasCollectionType() =
    PsiType.getTypeByName("java.util.Collection", project, resolveScope).isAssignableFrom(type)

class ReplaceDataOnEntityQuickFix(psiClass: PsiClass) : LocalQuickFixOnPsiElement(psiClass) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Replace @Data with safer parts"

    override fun invoke(project: Project, file: PsiFile, psiClass: PsiElement, endElement: PsiElement) {
        if (psiClass !is PsiClass) return
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project, "Replace @Data With Safer Parts", "Live-Coding", {
                val javaPsi = JavaPsiFacade.getInstance(project)
                psiClass.removeAnnotation("lombok.Data")
                psiClass.setAnnotation("lombok.Getter")
                psiClass.setAnnotation("lombok.Setter")?.let {
                    val commentOnSetter = javaPsi.elementFactory.createCommentFromText("// consider encapsulating changes", psiClass)
                    it.addSiblingAfter(commentOnSetter)
                }

                psiClass.setAnnotation("lombok.ToString")
                ExcludeCollectionsFromToStringQuickFix.excludeCollectionFields(psiClass)
            })
        }
    }
}

class ExcludeCollectionsFromToStringQuickFix(psiClass: PsiClass) : LocalQuickFixOnPsiElement(psiClass) {
    override fun getFamilyName() = "Live-Coding"
    override fun getText() = "Exclude collections from generated @ToString"

    override fun invoke(project: Project, file: PsiFile, psiClass: PsiElement, endElement: PsiElement) {
        if (psiClass !is PsiClass) return
        excludeCollectionFields(psiClass)
    }

    companion object {
        fun excludeCollectionFields(psiClass: PsiClass) {
            // TODO what about fields from superclasses : Postpone as it's not likely to have a collection in supertype
            for (field in psiClass.fields) {
                if (field.hasCollectionType() && !field.hasAnnotation("lombok.ToString.Include")) {
                    field.setAnnotation("lombok.ToString.Exclude", false)
                }
            }
        }
    }

}

class RemoveHashCodeEqualsQuickFix(psiClass: PsiClass) : LocalQuickFixOnPsiElement(psiClass) {
    override fun getFamilyName() = "Live-Coding"

    override fun getText() = "Remove @EqualsAndHashCode from this @Entity"

    override fun invoke(project: Project, file: PsiFile, psiClass: PsiElement, endElement: PsiElement) {
        if (psiClass !is PsiClass) return
        psiClass.removeAnnotation("lombok.EqualsAndHashCode")
    }
}
