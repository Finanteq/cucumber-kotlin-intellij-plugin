package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil
import org.jetbrains.plugins.cucumber.psi.GherkinFileType
import org.jetbrains.plugins.cucumber.psi.GherkinStep

class CucumberKotlinStepDefinitionSearch : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {

    override fun execute(queryParameters: ReferencesSearch.SearchParameters,
                         consumer: Processor<in PsiReference>): Boolean {
        val myElement = queryParameters.elementToSearch as? PsiMethod ?: return true
        val isStepDefinition = ReadAction.compute<Boolean, RuntimeException> { CucumberJavaUtil.isStepDefinition(myElement) }
        if (!isStepDefinition) {
            return true
        }

        val regexp = ReadAction.compute<String?, RuntimeException> {
            CucumberJavaUtil.getCucumberStepAnnotation(myElement)?.let { getPatternFromStepDefinition(it) }
        } ?: return true

        val scope = (queryParameters.effectiveSearchScope as? GlobalSearchScope)?.let { FasterGherkinSearchScope(it) } ?: queryParameters.effectiveSearchScope
        return CucumberUtil.findPossibleGherkinElementUsages(myElement, regexp, FasterMyReferenceCheckingProcessor(consumer, regexp.toRegex()), scope)
    }


    private fun getPatternFromStepDefinition(stepAnnotation: PsiAnnotation): String? {
        var result: String? = null
        if (stepAnnotation.parameterList.attributes.isNotEmpty()) {
            val annotationValue = stepAnnotation.parameterList.attributes[0].value
            if (annotationValue != null) {
                val patternLiteral = annotationValue.text
                if (patternLiteral != null) {
                    result = patternLiteral.substring(1, patternLiteral.length - 1).replace("\\\\", "\\")
                }
            }
        }
        return result
    }

}

class FasterGherkinSearchScope(baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope) {
    override fun contains(file: VirtualFile): Boolean {
        return file.fileType == GherkinFileType.INSTANCE && super.contains(file)
    }
}

private class FasterMyReferenceCheckingProcessor(
        val myConsumer: Processor<in PsiReference>,
        val regex: Regex) : TextOccurenceProcessor {

    override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
        val parent = element.parent
        val result = executeInternal(element)
        return if (result && parent != null) {
            executeInternal(parent)
        } else result
    }

    /**
     * Gets all injected reference and checks if some of them points to [.myElementToFind]
     *
     * @param referenceOwner element with injected references
     * @return true if element found and consumed
     */
    private fun executeInternal(referenceOwner: PsiElement): Boolean {
        if (referenceOwner is GherkinStep && referenceOwner.stepName.matches(regex)) {
            for (ref in referenceOwner.references) {
                if (ref != null) {
                    if (!myConsumer.process(ref)) {
                        return false
                    }
                }
            }
        }
        return true
    }
}


