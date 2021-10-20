package com.finanteq.plugins.idea.cucumber.kotlin.run.android

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.LayeredIcon
import icons.CucumberIcons
import icons.StudioIcons
import javax.swing.Icon
import javax.swing.SwingConstants

private const val displayName = "Cucumber Android"

class CucumberKotlinAndroidRunConfigurationType : ConfigurationTypeBase(
    "CucumberKotlinAndroidRunConfigurationType",
    displayName,
    icon = NotNullLazyValue.createValue {
        val icon = LayeredIcon(2)
        icon.setIcon(StudioIcons.Shell.Filetree.ANDROID_PROJECT, 0)
        icon.setIcon(LayeredIcon(CucumberIcons.Cucumber).scale(0.6f), 1, SwingConstants.NORTH_EAST)
        icon
    }
) {

    val factory: ConfigurationFactory = object : ConfigurationFactory(this) {
        override fun getIcon(): Icon {
            return this@CucumberKotlinAndroidRunConfigurationType.icon!!
        }

        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return CucumberKotlinAndroidRunConfiguration(project, this)
        }

        override fun getId(): String {
            return displayName
        }
    }

    init {
        addFactory(factory)
    }

    companion object {

        fun getInstance(): CucumberKotlinAndroidRunConfigurationType {
            return findConfigurationType(CucumberKotlinAndroidRunConfigurationType::class.java)
        }
    }
}