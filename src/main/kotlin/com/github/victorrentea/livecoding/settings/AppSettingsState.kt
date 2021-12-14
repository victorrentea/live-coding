package com.github.victorrentea.livecoding.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.lang.IllegalArgumentException

@State(
    name = "com.github.victorrentea.livecoding.settings.AppSettingsState",
    storages = [Storage("LiveCodingPluginSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {
//    var userId = "John Q. Public"
//    var ideaStatus = false
    @Volatile
    var staticImports = mapOf<String, String>()
        get
    var staticImportsList: List<String>
        get() = staticImports.map { it.value + "." + it.key  }
        set(newList)  {
            staticImports = parseStaticImports(newList)
        }
    init {
        staticImportsList = listOf(
            "org.assertj.core.api.Assertions.assertThat",
            "java.util.stream.Collectors.toSet",
            "java.util.stream.Collectors.toList",
            "java.util.stream.Collectors.toMap",
            "java.util.stream.Collectors.groupingBy",
            "java.util.function.Predicate.not",
            "org.mockito.Mockito.mock",
            "org.mockito.Mockito.when",
            "org.mockito.Mockito.verify",
            "java.lang.System.currentTimeMillis",
            "org.mockito.ArgumentMatchers.anyInt",
            "org.mockito.ArgumentMatchers.any",
            "org.mockito.ArgumentMatchers.anyString",
            "org.mockito.ArgumentMatchers.anyLong",
            "java.util.concurrent.TimeUnit.MILLISECONDS",
            "java.time.Duration.ofSeconds",
            "java.time.Duration.ofMillis",
            "java.util.concurrent.CompletableFuture.completedFuture"
        )
    }

    private fun parseStaticImports(lines: List<String>) =
        lines
            .filter { it.contains(".") }
            .map { it.trim() }
//            .also { if (!it.contains(".")) throw IllegalArgumentException("Each line should be of the form org.package.Class.staticMethod or .*") }
            .associate { it.substringAfterLast(".") to it.substringBeforeLast(".") }
            .toMap()

    override fun getState(): AppSettingsState = this
    override fun loadState(state: AppSettingsState) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        fun getInstance(): AppSettingsState {
            return ApplicationManager.getApplication().getService(AppSettingsState::class.java)
        }
    }
}