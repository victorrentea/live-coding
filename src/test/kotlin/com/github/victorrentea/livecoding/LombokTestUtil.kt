package com.github.victorrentea.livecoding

import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ContentEntry
import com.intellij.testFramework.fixtures.MavenDependencyUtil
import com.github.victorrentea.livecoding.LombokTestUtil
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IdeaTestUtil

object LombokTestUtil {
    const val LOMBOK_MAVEN_COORDINATES = "org.projectlombok:lombok:1.18.22"
    val LOMBOK_DESCRIPTOR: DefaultLightProjectDescriptor = object : DefaultLightProjectDescriptor() {
        fun configureModule(module: Module?, model: ModifiableRootModel, contentEntry: ContentEntry?) {
            MavenDependencyUtil.addFromMaven(model, LOMBOK_MAVEN_COORDINATES)
//            MavenDependencyUtil.addFromMaven(model, "com.google.guava:guava:27.0.1-jre")
            MavenDependencyUtil.addFromMaven(model, "org.slf4j:slf4j-api:1.7.30")
            model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_8
        }

        override fun getSdk(): Sdk? {
            return IdeaTestUtil.getMockJdk18()
        }
    }
    val LOMBOK_NEW_DESCRIPTOR: DefaultLightProjectDescriptor = object : DefaultLightProjectDescriptor() {
        fun configureModule(module: Module?, model: ModifiableRootModel, contentEntry: ContentEntry?) {
            MavenDependencyUtil.addFromMaven(model, LOMBOK_MAVEN_COORDINATES)
            model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel =
                LanguageLevel.HIGHEST
        }
    }
    val LOMBOK_OLD_DESCRIPTOR: DefaultLightProjectDescriptor = object : DefaultLightProjectDescriptor() {
        fun configureModule(module: Module?, model: ModifiableRootModel, contentEntry: ContentEntry?) {
            MavenDependencyUtil.addFromMaven(model, "org.projectlombok:lombok:1.18.2")
            model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel =
                LanguageLevel.JDK_1_8
        }
    }
}