package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.cucumber.CucumberJvmExtensionPoint
import org.jetbrains.plugins.cucumber.java.CucumberJavaExtension
import org.jetbrains.plugins.cucumber.steps.CucumberStepHelper

class KotlinGherkinAnnotator : Annotator {

    companion object {
        private val point = CucumberJvmExtensionPoint.EP_NAME.getPoint(null)
        private var removed = false
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!removed) {
            CucumberStepHelper.getCucumberExtensions()
            point.unregisterExtension(CucumberJavaExtension::class.java)
            removed = true
        }
    }
}