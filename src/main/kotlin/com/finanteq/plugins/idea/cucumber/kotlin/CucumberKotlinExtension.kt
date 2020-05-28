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

//    override fun isStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
//        return child is FakeFileForLightClass || child is KtFile
//    }
//
//    override fun isWritableStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
//        if (isStepLikeFile(child, parent))
//            return super.isWritableStepLikeFile(child, parent)
//        return false
//    }
//
//    override fun getStepDefinitionContainers(featureFile: GherkinFile): Collection<PsiFile> {
//        return super.getStepDefinitionContainers(featureFile)
//    }
}
