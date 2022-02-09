package com.github.victorrentea.livecoding.assertj

import com.github.victorrentea.livecoding.FrameworkDetector
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import com.siyeh.ig.psiutils.ImportUtils

class MigrateToAssertJInspection : BaseInspection() {
    companion object {
        const val INSPECTION_NAME = "JUnit assertion can be migrated to AssertJ";
        const val FIX_NAME = "Replace with AssertJ assertion"
        private val log = logger<MigrateToAssertJInspection>()

        private fun tryImportStatically(fqMethodName: String, context: PsiElement): String {
            val fqClassName = fqMethodName.substringBeforeLast(".")
            val methodName = fqMethodName.substringAfterLast(".")

            val staticImported = ImportUtils.addStaticImport(fqClassName, methodName, context)

            val methodPrefix = if (staticImported) "" else fqClassName

            return methodPrefix + methodName
        }

    }

    override fun shouldInspect(file: PsiFile?) =
        file?.let { FrameworkDetector.assertjIsPresent(it) } == true

    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME

    override fun buildVisitor() = MigrateToAssertJVisitor()

    class MigrateToAssertJVisitor : BaseInspectionVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
            val calledPsiMethod = expression?.methodExpression?.resolve() as? PsiMethod ?: return

            if (!calledPsiMethod.hasModifierProperty(PsiModifier.STATIC)) return
            val classQName = calledPsiMethod.containingClass?.qualifiedName ?: return

            if (classQName !in listOf("org.junit.jupiter.api.Assertions", "org.junit.Assert")) return

            val descr = if (calledPsiMethod.parameterList.parameters.last()
                    .let{it.name == "message" && it.type.equalsToText("java.lang.String")}) {
                "\n.describedAs(" + expression.argumentList.expressions.last().text+")\n"
            } else ""

            val args = expression.argumentList.expressions
            val arg0 = args[0].text
            val arg1 = args.getOrNull(1)?.text
            val migratedCode = "org.assertj.core.api.Assertions." + when (calledPsiMethod.name)  {
                "assertEquals" -> "assertThat($arg1)$descr.isEqualTo($arg0)"
                "assertNotEquals" -> "assertThat($arg1)$descr.isNotEqualTo($arg0)"
                "assertTrue" -> "assertThat($arg0)$descr.isTrue()"
                "assertFalse" -> "assertThat($arg0)$descr.isFalse()"
                "assertNull" -> "assertThat($arg0)$descr.isNull()"
                "assertNotNull" -> "assertThat($arg0)$descr.isNotNull()"
                "assertThrows" -> if (descr == "") "assertThatThrownBy($arg1).isInstanceOf($arg0)"
                                    else "assertThatThrownBy($arg1, "+expression.argumentList.expressions.last().text+").isInstanceOf($arg0)"
                else -> return
            }

            registerError(expression.methodExpression, migratedCode)
        }
    }

    override fun buildFix(vararg infos: Any?) = MigrateToAssertJFix(infos[0] as String)

    class MigrateToAssertJFix(private val rawMigratedCode: String) : InspectionGadgetsFix() {
        override fun getFamilyName(): String {
            return FIX_NAME
        }

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            if (project == null) return
            val methodCall = descriptor?.psiElement?.parent as? PsiMethodCallExpression ?: return

            val fqMethodPart = rawMigratedCode.substringBefore("(")
            val shortMethod = tryImportStatically(fqMethodPart, methodCall)
            val newExpressionText = shortMethod + "(" + rawMigratedCode.substringAfter("(")

            val newExpression =
                JavaPsiFacade.getElementFactory(project).createExpressionFromText(newExpressionText, methodCall)

            methodCall.replace(newExpression)
        }

    }

}