package com.github.victorrentea.livecoding.varie

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiIfStatement
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix


class IntroduceGuardInspection : BaseInspection() {

    override fun buildVisitor() = IntroduceGuardVisitor()

    override fun buildErrorString(vararg infos: Any?): String {
        return "The alternate (else) branch is much lighter"
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix? {
        return MyFix()
    }

    class IntroduceGuardVisitor : BaseInspectionVisitor() {
        override fun visitIfStatement(ifStatement: PsiIfStatement?) {
            if (ifStatement == null) return

            registerError(ifStatement.firstChild, "A")
        }
    }

    class MyFix : InspectionGadgetsFix() {
        override fun getFamilyName(): String {
            return "Invert 'if' to simplify method"
        }

        override fun doFix(project: Project?, descriptor: ProblemDescriptor?) {
            println("TODO")
        }
    }
}


