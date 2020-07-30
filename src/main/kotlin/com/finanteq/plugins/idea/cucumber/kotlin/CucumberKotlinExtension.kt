package com.finanteq.plugins.idea.cucumber.kotlin

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.java.CucumberJavaExtension

class CucumberKotlinExtension : CucumberJavaExtension() {


    private val bddFrameworkType = BDDFrameworkType(KotlinFileType.INSTANCE)

    private val kotlinStepDefinitionCreator = KotlinStepDefinitionCreator()

    override fun getStepFileType(): BDDFrameworkType {
        return bddFrameworkType
    }

    override fun getStepDefinitionCreator(): StepDefinitionCreator {
        return kotlinStepDefinitionCreator
    }
}
