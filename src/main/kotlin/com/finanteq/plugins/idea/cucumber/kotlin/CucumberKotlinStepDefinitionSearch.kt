package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil

class CucumberKotlinStepDefinitionSearch : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(queryParameters: ReferencesSearch.SearchParameters,
                         consumer: Processor<PsiReference>): Boolean {
        val psiMethod = queryParameters.elementToSearch as? PsiMethod
        val myElement = if (psiMethod != null) psiMethod else return true
        val isStepDefinition = ReadAction.compute<Boolean, RuntimeException> { CucumberJavaUtil.isStepDefinition(myElement) }
        if (!isStepDefinition) {
            return true
        }

        val stepAnnotation = ReadAction.compute<PsiAnnotation, RuntimeException> { CucumberJavaUtil.getCucumberStepAnnotation(myElement) }

        val regexp = getPatternFromStepDefinition(stepAnnotation) ?: return true
        return CucumberUtil.findGherkinReferencesToElement(myElement, regexp, consumer, queryParameters.effectiveSearchScope)
    }
}
