package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.FrameworkDetector
import com.github.victorrentea.livecoding.varie.IntroduceGuardInspection
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor

abstract class LombokJavaInspectionBase : BaseInspection() {

    override fun shouldInspect(file: PsiFile?): Boolean {
        return file?.project?.let{FrameworkDetector.hasLombokLibrary(it)} == true
    }
}