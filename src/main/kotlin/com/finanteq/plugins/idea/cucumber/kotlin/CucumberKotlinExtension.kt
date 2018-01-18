package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.java.CucumberJavaExtension

class CucumberKotlinExtension : CucumberJavaExtension() {

    override fun getStepFileType(): BDDFrameworkType {
        return BDDFrameworkType(KotlinFileType.INSTANCE)
    }

    override fun getStepDefinitionCreator(): StepDefinitionCreator {
        return KotlinStepDefinitionCreator()
    }

    override fun isStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        return child is FakeFileForLightClass || child is KtFile
    }

    override fun isWritableStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        if (isStepLikeFile(child, parent))
            return super.isWritableStepLikeFile(child, parent)
        return false
    }
}
