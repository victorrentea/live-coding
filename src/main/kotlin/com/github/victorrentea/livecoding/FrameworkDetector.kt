package com.github.victorrentea.livecoding

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

object FrameworkDetector {
    fun lombokIsPresent(element: PsiElement) =
        hasLombokLibrary(element.project)
//        JavaPsiFacade.getInstance(element.project)
//            .findClass("lombok.RequiredArgsConstructor", element.resolveScope) != null
    fun slf4jIsPresent(element: PsiElement) =
        JavaPsiFacade.getInstance(element.project)
            .findClass("org.slf4j.LoggerFactory", element.resolveScope) != null

    fun jpaIsPresent(element: PsiElement) =
        JavaPsiFacade.getInstance(element.project)
            .findClass("javax.persistence.Entity", element.resolveScope) != null


    private const val LOMBOK_PACKAGE = "lombok.experimental"

    fun hasLombokLibrary(project: Project): Boolean {
        if (project.isDefault || !project.isInitialized) {
            return false
        }
        ApplicationManager.getApplication().assertReadAccessAllowed()
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val aPackage = JavaPsiFacade.getInstance(project).findPackage(LOMBOK_PACKAGE)
            CachedValueProvider.Result(aPackage, ProjectRootManager.getInstance(project))
        } != null
    }
}