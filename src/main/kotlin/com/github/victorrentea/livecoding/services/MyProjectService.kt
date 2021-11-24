package com.github.victorrentea.livecoding.services

import com.intellij.openapi.project.Project
import com.github.victorrentea.livecoding.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
