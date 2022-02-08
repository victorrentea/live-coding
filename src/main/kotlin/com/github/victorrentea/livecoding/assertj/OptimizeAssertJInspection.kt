package com.github.victorrentea.livecoding.assertj

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

class OptimizeAssertJInspection : BaseInspection() {
    companion object {
        const val INSPECTION_NAME = "AssertJ assertion can be simplified";
        const val FIX_NAME = "Simplify AssertJ assertion"
        private val log = logger<OptimizeAssertJInspection>()
    }

    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME

    override fun buildVisitor() = OptimizeAssertJVisitor()

    class OptimizeAssertJVisitor : BaseInspectionVisitor() {
        override fun visitMethodCallExpression(assertThatCall: PsiMethodCallExpression?) {
            val calledPsiMethod = assertThatCall?.methodExpression?.resolve() as? PsiMethod ?: return
            if (assertThatCall.methodExpression.referenceName != "assertThat") return
            if (!calledPsiMethod.hasModifierProperty(PsiModifier.STATIC)) return
            if ((calledPsiMethod.containingClass?.qualifiedName ?: return) != "org.assertj.core.api.Assertions") return

            val parentCall=assertThatCall.parent.parent as? PsiMethodCallExpression ?: return
            if (parentCall.parent !is PsiExpressionStatement) return // chaining more than once, eg assertThat().bla().bla();
            val methodNameAfterAssertThat = parentCall.methodExpression.referenceName ?: return

            val assertThatArg = assertThatCall.argumentList.expressions[0]
            if (assertThatArg !is PsiMethodCallExpression) return
            val calledMethodOnArg = assertThatArg.resolveMethod()?:return
            val calledMethodOnArgFQN = calledMethodOnArg.containingClass?.qualifiedName + "." +calledMethodOnArg.name
            log.debug(calledMethodOnArgFQN)
            if (calledMethodOnArgFQN=="java.util.List.size" && methodNameAfterAssertThat == "isEqualTo") {
                val replacementCode =
                    assertThatCall.methodExpression.text + "(" + assertThatArg.methodExpression.qualifierExpression?.text + ").hasSize(" + parentCall.argumentList.expressions[0].text + ")"
                assertThatArg.methodExpression.referenceNameElement?.let { registerError(it, replacementCode) }
            }
        }
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix {
        return OptimizeAssertJFix(infos[0] as String)
    }
    class OptimizeAssertJFix(private val replacementCode: String) : InspectionGadgetsFix() {
        override fun getFamilyName() = FIX_NAME

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            if (project == null) return;
            if (descriptor?.psiElement == null) return;
            val fullStatementExpression = descriptor.psiElement.parent.parent.parent.parent.parent.parent
            log.debug("Replacing: '${fullStatementExpression.text}' with '$replacementCode'")

            val newExpression =
                JavaPsiFacade.getElementFactory(project).createExpressionFromText(replacementCode, fullStatementExpression)

            fullStatementExpression.replace(newExpression)
        }

    }


}