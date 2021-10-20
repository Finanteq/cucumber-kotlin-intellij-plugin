package com.finanteq.plugins.idea.cucumber.kotlin.run.android

import com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project

class CucumberKotlinAndroidRunConfiguration(project: Project?, factory: ConfigurationFactory?) : AndroidTestRunConfiguration(project, factory) {

    var suggestedName: String? = null

    override fun suggestedName(): String? {
        return suggestedName
    }
}