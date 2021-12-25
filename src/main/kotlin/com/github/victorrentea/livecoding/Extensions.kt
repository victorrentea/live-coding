package com.github.victorrentea.livecoding

import com.intellij.psi.PsiModifierList


fun PsiModifierList.hasAnyAnnotation(vararg annotationFqns: String) =
    annotationFqns.any { hasAnnotation(it) }