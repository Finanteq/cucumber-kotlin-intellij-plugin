package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.plugins.cucumber.inspections.CucumberStepDefinitionCreationContext
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.steps.CucumberStepsIndex

class CucumberKotlinStepsIndex(project: Project) : CucumberStepsIndex(project) {

    override fun getStepDefinitionContainers(featureFile: GherkinFile): MutableSet<CucumberStepDefinitionCreationContext> {
        val containers = super.getStepDefinitionContainers(featureFile)
        containers.removeIf { it.frameworkType?.fileType == JavaFileType.INSTANCE && it.psiFile is FakeFileForLightClass }
        return containers
    }
}