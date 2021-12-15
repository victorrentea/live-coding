package com.github.victorrentea.livecoding

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement

object FrameworkDetector {
    fun lombokIsPresent(element: PsiElement) =
        JavaPsiFacade.getInstance(element.project)
            .findClass("lombok.RequiredArgsConstructor", element.resolveScope) != null
    fun slf4jIsPresent(element: PsiElement) =
        JavaPsiFacade.getInstance(element.project)
            .findClass("org.slf4j.LoggerFactory", element.resolveScope) != null

    fun jpaIsPresent(element: PsiElement) =
        JavaPsiFacade.getInstance(element.project)
            .findClass("javax.persistence.Entity", element.resolveScope) != null

}