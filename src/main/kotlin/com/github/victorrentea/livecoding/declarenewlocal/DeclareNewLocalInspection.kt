package com.github.victorrentea.livecoding.declarenewlocal

import com.github.victorrentea.livecoding.lombok.AddRequiredArgsConstructorInspection
import com.github.victorrentea.livecoding.lombok.ReplaceWithRequiredArgsConstructorInspection
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElementVisitor
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

class DeclareNewLocalInspection : BaseInspection() {
    companion object {
        const val INSPECTION_NAME = "Local variable semantics might be confusing";
        const val FIX_NAME = "Declare new variable here"
    }

    override fun buildErrorString(vararg infos: Any?) = INSPECTION_NAME

    override fun buildVisitor(): BaseInspectionVisitor {
        return DeclareNewLocalVisitor()
    }

    override fun buildFix(vararg infos: Any?): InspectionGadgetsFix {
        return DeclareNewLocalFix()
    }

}