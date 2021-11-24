package com.github.victorrentea.livecoding.lombok

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

object FrameworkDetector {
    fun lombokIsPresent(project: Project, element: PsiElement) =
        JavaPsiFacade.getInstance(project)
            .findClass("lombok.RequiredArgsConstructor", element.resolveScope) != null

    fun visibleForTestingIsPresent(project: Project, element: PsiElement) =
        JavaPsiFacade.getInstance(project)
            .findClass("com.google.common.annotations.VisibleForTesting", element.resolveScope) != null
}