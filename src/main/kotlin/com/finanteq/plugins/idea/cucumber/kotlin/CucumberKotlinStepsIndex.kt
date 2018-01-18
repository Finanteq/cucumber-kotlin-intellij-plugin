package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.steps.CucumberStepsIndex

class CucumberKotlinStepsIndex(project: Project) : CucumberStepsIndex(project) {

    override fun getStepDefinitionContainers(featureFile: GherkinFile): MutableSet<Pair<PsiFile, BDDFrameworkType>> {
        val containers = super.getStepDefinitionContainers(featureFile)
        containers.removeIf { it.second.fileType == JavaFileType.INSTANCE && it.first is FakeFileForLightClass }
        return containers
    }
}