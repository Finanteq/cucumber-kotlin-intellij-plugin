package com.finanteq.plugins.idea.cucumber.kotlin.run

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.util.elementType
import org.jetbrains.plugins.cucumber.psi.GherkinExamplesBlock
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinTableRow
import org.jetbrains.plugins.cucumber.psi.GherkinTokenTypes
import org.jetbrains.plugins.cucumber.psi.impl.GherkinTableHeaderRowImpl

class CucumberKotlinScenarioOutlineExampleRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {

        return if (element !is LeafElement) {
            null
        } else {
            val psiFile = element.containingFile
            if (psiFile !is GherkinFile) {
                null
            } else {
                if (element.isExampleInExamples()) {

                    return getInfo()
                }
                return null
            }
        }
    }


    private fun getInfo(): Info {
        return withExecutorActions(getTestStateIcon(null, true))
    }
}

fun PsiElement.isExampleInExamples() = (elementType == GherkinTokenTypes.PIPE
        && parent is GherkinTableRow
        && parent !is GherkinTableHeaderRowImpl
        && prevSibling == null
        && parent.parent.parent is GherkinExamplesBlock)