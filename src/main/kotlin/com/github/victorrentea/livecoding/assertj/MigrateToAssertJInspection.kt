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
        const val FIX_NAME = "Replace with 'org.assertj.core.api.Assertions' method call (AssertJ)"
        private val log = logger<MigrateToAssertJInspection>()


        val SIGNATURES = mapOf<String, (PsiMethodCallExpression) -> String>(
            "assertEquals(Object,Object)" to ::migrateEquals,
            "assertEquals(int,Integer)" to ::migrateEquals,
            "assertEquals(int,int)" to ::migrateEquals,
            "assertEquals(float,float)" to ::migrateEquals,
            "assertEquals(double,double)" to ::migrateEquals,
            "assertEquals(char,char)" to ::migrateEquals,
            "assertEquals(char,char)" to ::migrateEquals,
            "assertEquals(long,long)" to ::migrateEquals,
            "assertEquals(long,Long)" to ::migrateEquals,
            "assertFalse(boolean)" to ::migrateFalse,
            "assertTrue(boolean)" to ::migrateTrue,
            "assertNull(Object)" to ::migrateNull,
            "assertNotNull(Object)" to ::migrateNotNull,
            "assertThrows(Class<T>,Executable)" to ::migrateThrows,
        )

        private fun tryImportStatically(fqMethodName: String, context: PsiElement): String {
            val fqClassName = fqMethodName.substringBeforeLast(".")
            val methodName = fqMethodName.substringAfterLast(".")

            val staticImported = ImportUtils.addStaticImport(fqClassName, methodName, context)

            val methodPrefix = if (staticImported) "" else fqClassName

            return methodPrefix + methodName
        }

        private fun migrateEquals(call: PsiMethodCallExpression): String {
            return "org.assertj.core.api.Assertions.assertThat(" + call.argumentList.expressions[1].text +
                    ").isEqualTo(" + call.argumentList.expressions[0].text + ")"
        }

        private fun migrateNull(call: PsiMethodCallExpression): String {
            return "org.assertj.core.api.Assertions.assertThat(" + call.argumentList.expressions[0].text + ").isNull()"
        }

        private fun migrateFalse(call: PsiMethodCallExpression): String {
            return "org.assertj.core.api.Assertions.assertThat(" + call.argumentList.expressions[0].text + ").isFalse()"
        }

        private fun migrateTrue(call: PsiMethodCallExpression): String {
            return "org.assertj.core.api.Assertions.assertThat(" + call.argumentList.expressions[0].text + ").isTrue()"
        }

        private fun migrateNotNull(call: PsiMethodCallExpression): String {
            return "org.assertj.core.api.Assertions.assertThat(" + call.argumentList.expressions[0].text + ").isNotNull()"
        }

        private fun migrateThrows(call: PsiMethodCallExpression): String {
            return "org.assertj.core.api.Assertions.assertThatThrownBy(" + call.argumentList.expressions[1].text + ")\n" +
                    ".isInstanceOf(" + call.argumentList.expressions[0].text + ")"
        }
    }

    override fun shouldInspect(file: PsiFile?): Boolean {
        return file?.let { FrameworkDetector.assertjIsPresent(it) } == true
    }

    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME

    override fun buildVisitor(): BaseInspectionVisitor {
        return MigrateToAssertJVisitor()
    }

    class MigrateToAssertJVisitor : BaseInspectionVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
            val calledPsiMethod = expression?.methodExpression?.resolve() as? PsiMethod ?: return

            if (!calledPsiMethod.hasModifierProperty(PsiModifier.STATIC)) return
            val classQName = calledPsiMethod.containingClass?.qualifiedName ?: return
            if (classQName != "org.junit.jupiter.api.Assertions" && classQName != "org.junit.Assert") return

            val typeStr = calledPsiMethod.parameterList.parameters
                .map { it.typeElement?.type?.presentableText }.joinToString(",")

            val methodSignature = calledPsiMethod.name + "(" + typeStr + ")"

            log.debug("Found $methodSignature")

            if (SIGNATURES.containsKey(methodSignature)) {
                registerError(expression.methodExpression, methodSignature)
            }
        }
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix {
        return MigrateToAssertJFix(infos[0] as String)
    }

    class MigrateToAssertJFix(private val signature: String) : InspectionGadgetsFix() {
        override fun getFamilyName(): String {
            return FIX_NAME
        }

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            if (project == null) return
            val methodCall = descriptor?.psiElement?.parent as? PsiMethodCallExpression ?: return
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(descriptor.psiElement.containingFile) ?: return
            val transformFunction = SIGNATURES[signature] ?: return
//            WriteCommandAction.runWriteCommandAction(project,
//                FIX_NAME, "Live-Coding", {
            val rawExpressionText = transformFunction(methodCall)

            val fqMethodPart = rawExpressionText.substringBefore("(")
            val shortMethod = tryImportStatically(fqMethodPart, methodCall)
            val newExpressionText = shortMethod + "(" + rawExpressionText.substringAfter("(")

            val newExpression =
                JavaPsiFacade.getElementFactory(project).createExpressionFromText(newExpressionText, methodCall)

            methodCall.replace(newExpression)
//                    documentManager.doPostponedOperationsAndUnblockDocument(document)
//
//                    JavaCodeStyleManager.getInstance(project).optimizeImports(newExpression.containingFile)
//
//                    documentManager.doPostponedOperationsAndUnblockDocument(document)
//
//                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(newExpression)
//                })
        }

    }

}