package com.github.victorrentea.livecoding.declarenewlocal

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class DeclareNewLocalInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_NAME = "Local variable semantics might be confusing";
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DeclareNewLocalVisitor(holder)
    }
}