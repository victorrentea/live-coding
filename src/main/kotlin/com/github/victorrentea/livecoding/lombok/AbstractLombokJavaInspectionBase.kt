package com.github.victorrentea.livecoding.lombok

import com.github.victorrentea.livecoding.FrameworkDetector
import com.intellij.psi.PsiFile
import com.siyeh.ig.BaseInspection

abstract class AbstractLombokJavaInspectionBase : BaseInspection() {

    override fun shouldInspect(file: PsiFile?): Boolean {
        return file?.project?.let{FrameworkDetector.hasLombokLibrary(it)} == true
    }
}