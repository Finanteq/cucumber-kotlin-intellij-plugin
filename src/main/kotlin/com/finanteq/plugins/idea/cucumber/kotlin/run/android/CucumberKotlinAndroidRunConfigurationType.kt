package com.finanteq.plugins.idea.cucumber.kotlin.run.android

import com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfigurationType
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.configurations.*
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.LayeredIcon
import icons.CucumberIcons
import icons.StudioIcons
import javax.swing.SwingConstants

private const val CONFIGURATION_NAME = "Cucumber Android"

class CucumberKotlinAndroidRunConfigurationType : ConfigurationTypeBase(
    "CucumberKotlinAndroidRunConfigurationType",
    CONFIGURATION_NAME,
    icon = NotNullLazyValue.createValue {
        val icon = LayeredIcon(2)
        icon.setIcon(StudioIcons.Shell.Filetree.ANDROID_PROJECT, 0)
        icon.setIcon(LayeredIcon(CucumberIcons.Cucumber).scale(0.6f), 1, SwingConstants.NORTH_EAST)
        icon
    }
) {

    val factory: ConfigurationFactory = object : ConfigurationFactory(this) {

        private val androidDelegate by lazy { AndroidTestRunConfigurationType.getInstance().factory }

        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return CucumberKotlinAndroidRunConfiguration(project, this)
        }

        override fun getOptionsClass(): Class<out BaseState> {
            return CucumberKotlinAndroidRunConfigurationOptions::class.java
        }

        override fun isApplicable(project: Project): Boolean {
            return androidDelegate.isApplicable(project)
        }

        override fun getId(): String {
            return CONFIGURATION_NAME
        }

        override fun configureBeforeRunTaskDefaults(providerID: Key<out BeforeRunTask<BeforeRunTask<*>>>?, task: BeforeRunTask<out BeforeRunTask<*>>?) {
            androidDelegate.configureBeforeRunTaskDefaults(providerID, task)
        }

        override fun getSingletonPolicy(): RunConfigurationSingletonPolicy {
            return androidDelegate.singletonPolicy
        }
    }

    init {
        addFactory(factory)
    }

    companion object {

        fun getInstance(): CucumberKotlinAndroidRunConfigurationType {
            return runConfigurationType()
        }
    }
}