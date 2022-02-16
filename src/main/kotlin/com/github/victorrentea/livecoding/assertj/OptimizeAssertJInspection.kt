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

            val parentCall = assertThatCall.parent.parent as? PsiMethodCallExpression ?: return
            if (parentCall.parent !is PsiExpressionStatement) return // chaining more than once, eg assertThat().bla().bla();
            val gammaText = parentCall.methodExpression.referenceName ?: return

            val assertThatArg = assertThatCall.argumentList.expressions[0]
            if (assertThatArg !is PsiMethodCallExpression) return
            val betaElement = assertThatArg.methodExpression.referenceNameElement ?: return
            val calledMethodOnArg = assertThatArg.resolveMethod() ?: return
            val betaFQN = calledMethodOnArg.containingClass?.qualifiedName + "." + calledMethodOnArg.name
            log.debug(betaFQN)

            //  assertThat(<alfa>.<beta>()).<gamma>(..);

            val alfaText = assertThatArg.methodExpression.qualifierExpression?.text
            val gammaParamText = parentCall.argumentList.expressions.getOrNull(0)?.text?:return
            val replacementCode =
                if (gammaText == "isEqualTo" && betaFQN in listOf(
                        "java.util.List.size",
                        "java.util.Set.size",
                        "java.util.Map.size",
                        "java.lang.String.length",
                        "java.util.stream.Stream.count"
                    )
                ) {
                    if (gammaParamText == "0") {
                        "($alfaText).isEmpty()" // TODO Victor 2022-02-09: move to another separate inspection
                    } else  {
                        "($alfaText).hasSize($gammaParamText)"
                    }
                } else {
                    null
                }
            replacementCode?.let {
                registerError(
                    betaElement, assertThatCall.methodExpression.text + it
                )
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
                JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText(replacementCode, fullStatementExpression)

            fullStatementExpression.replace(newExpression)
        }

    }


}