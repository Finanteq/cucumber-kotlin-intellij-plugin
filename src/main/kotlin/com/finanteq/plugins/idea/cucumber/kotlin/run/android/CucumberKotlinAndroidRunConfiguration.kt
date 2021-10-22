package com.finanteq.plugins.idea.cucumber.kotlin.run.android

import com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.OptionTag

class CucumberKotlinAndroidRunConfiguration(project: Project?, factory: ConfigurationFactory?) : AndroidTestRunConfiguration(project, factory) {

    override fun getOptions(): CucumberKotlinAndroidRunConfigurationOptions {
        return super.getOptions() as CucumberKotlinAndroidRunConfigurationOptions
    }

    fun setSuggestedName(suggestedName: String) {
        options.suggestedName = suggestedName
    }

    fun setExtraOptions(extraOptions: String) {
        options.extraInstrumentationOptions = extraOptions
        EXTRA_OPTIONS = extraOptions
    }

    override fun setOptionsFromConfigurationFile(state: BaseState) {
        super.setOptionsFromConfigurationFile(state)
        EXTRA_OPTIONS = options.extraInstrumentationOptions.orEmpty()
    }

    override fun suggestedName(): String? {
        return options.suggestedName
    }

    override fun getActionName(): String {
        return name
    }
}

class CucumberKotlinAndroidRunConfigurationOptions : ModuleBasedConfigurationOptions() {
    @get:OptionTag(tag = "suggestedName", nameAttribute = "")
    var suggestedName by string()

    @get:OptionTag(tag = "extraInstrumentationOptions", nameAttribute = "")
    var extraInstrumentationOptions by string("")
}