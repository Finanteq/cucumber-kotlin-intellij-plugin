package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil.getPatternFromStepDefinition
import org.jetbrains.plugins.cucumber.psi.GherkinFileType

class CucumberKotlinMethodUsageSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {

        val scope = queryParameters.getEffectiveSearchScope() as? GlobalSearchScope ?: return

        val method = queryParameters.getMethod()

        val stepAnnotation = CucumberJavaUtil.getCucumberStepAnnotation(method)
        val regexp = (if (stepAnnotation != null) getPatternFromStepDefinition(stepAnnotation) else null) ?: return
        val word = CucumberUtil.getTheBiggestWordToSearchByIndex(regexp)
        if (StringUtil.isEmpty(word)) {
            return
        }

        val restrictedScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope,
                GherkinFileType.INSTANCE)
        ReferencesSearch.search(ReferencesSearch.SearchParameters(method, restrictedScope, false, queryParameters.getOptimizer())).forEach(consumer)

    }


}


fun getPatternFromStepDefinition(stepAnnotation: PsiAnnotation): String? {
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