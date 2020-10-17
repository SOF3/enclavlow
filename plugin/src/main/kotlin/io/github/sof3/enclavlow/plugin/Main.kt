package io.github.sof3.enclavlow.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class Main : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("enclavlow") { task ->
            task.dependsOn("classes")
            task.doLast {

            }
        }
    }
}
