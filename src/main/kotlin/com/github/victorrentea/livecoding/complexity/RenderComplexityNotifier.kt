package com.github.victorrentea.livecoding.complexity

import com.intellij.util.messages.Topic

interface RenderComplexityNotifier {
    companion object {
        @Topic.ProjectLevel
        val CHANGE_ACTION_TOPIC = Topic.create("custom name", RenderComplexityNotifier::class.java)
    }
    fun complexityComputed(virtualFileUrl:String, complexities:List<ComplexityToDisplayInFile>)
}