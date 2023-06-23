package com.finanteq.plugins.idea.cucumber.kotlin.run

import com.intellij.execution.JavaExecutionUtil
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.plugins.cucumber.java.run.CucumberJavaRunConfiguration
import org.jetbrains.plugins.cucumber.java.run.CucumberJavaScenarioRunConfigurationProducer

class CucumberJavaScenarioOutlineExampleRunConfigurationProducer : CucumberJavaScenarioRunConfigurationProducer() {

    override fun getConfigurationName(context: ConfigurationContext): String {
        val configurationName = super.getConfigurationName(context)
        return "$configurationName, at ${context.getLineNumber()}"
    }

    override fun setupConfigurationFromContext(configuration: CucumberJavaRunConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        if (super.setupConfigurationFromContext(configuration, context, sourceElement)) {
            val pathWithLineNumber = pathWithLineNumberIfIsExample(context, filePath = { configuration.filePath })
            if (pathWithLineNumber != null) {
                configuration.filePath = pathWithLineNumber
                return true
            }
        }
        return false
    }

    private fun pathWithLineNumberIfIsExample(context: ConfigurationContext, filePath: () -> String): String? {
        val psiElement = context.location?.psiElement ?: return null
        if (!psiElement.isExampleInExamples()) return null

        val lineNumber = psiElement.getLineNumber() + 1
        return "${filePath()}:$lineNumber"
    }

    private fun ConfigurationContext.getLineNumber(): Int? {
        return location?.psiElement?.getLineNumber()?.let { it + 1 }
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return other.isProducedBy(CucumberJavaScenarioRunConfigurationProducer::class.java)
    }

    override fun isConfigurationFromContext(runConfiguration: CucumberJavaRunConfiguration, context: ConfigurationContext): Boolean {
        val location = context.location
        return if (location == null) {
            false
        } else {
            val classLocation = JavaExecutionUtil.stepIntoSingleClass(location)
            if (classLocation == null) {
                false
            } else {
                val fileToRun = getFileToRun(context)
                if (fileToRun == null) {
                    false
                } else if (pathWithLineNumberIfIsExample(context, filePath = { fileToRun.path }) != runConfiguration.filePath) {
                    false
                } else if (!Comparing.strEqual(getNameFilter(context), runConfiguration.nameFilter)) {
                    false
                } else {
                    val configurationModule = (runConfiguration.configurationModule as JavaRunConfigurationModule).module
                    Comparing.equal(classLocation.module, configurationModule)
                }
            }
        }
    }
}