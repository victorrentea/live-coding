package com.github.victorrentea.livecoding.varie

import com.github.victorrentea.livecoding.hasAnyAnnotation
import com.intellij.psi.PsiMethod
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor

class DontOverrideBeforeInspection : BaseInspection() {
    override fun buildErrorString(vararg infos: Any?) =
        "Dangerous override of inherited before method."

    override fun buildVisitor() = DontOverrideBeforeVisitor()

    class DontOverrideBeforeVisitor : BaseInspectionVisitor() {
        override fun visitMethod(method: PsiMethod?) {
            if (method == null) return

            if (!method.modifierList.hasAnyAnnotation(
                    "org.junit.jupiter.api.BeforeEach",
                    "org.junit.Before")) return

            if (method.findSuperMethods().isEmpty()) return

            method.nameIdentifier?.let{
                registerError(it)
            }
        }
    }
}


