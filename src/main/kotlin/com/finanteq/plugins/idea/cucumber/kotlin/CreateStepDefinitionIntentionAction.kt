package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.plugins.cucumber.inspections.CucumberCreateStepFix
import org.jetbrains.plugins.cucumber.psi.GherkinFile

class CreateStepDefinitionIntentionAction : IntentionAction {
    override fun startInWriteAction(): Boolean = false

    override fun getText(): String = "Create Kotlin step definition"

    override fun getFamilyName(): String = "Gherkin"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.isNotGherkinFile()) return false

        val gherkinFile = file as GherkinFile

        val keyword = editor.getLineText().trimStart().substringBefore(" ")
        return gherkinFile.stepKeywords.contains(keyword)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val fix = CucumberCreateStepFix()

        val caretOffset = editor.caretModel.offset
        for (feature in (file as GherkinFile).features) {
            for (scenario in feature.scenarios) {
                val step = scenario.steps.find { caretOffset in it.startOffset..it.endOffset } ?: continue
                val problemDescriptor = ProblemDescriptorBase(step, step, "", emptyArray(), ProblemHighlightType.POSSIBLE_PROBLEM, false, null, true, false)
                fix.applyFix(project, problemDescriptor)
                return
            }
        }

    }
}

class CreateStepDefinitionIntentionActionFactory : ExtensionFactory {
    override fun createInstance(factoryArgument: String, implementationClass: String?): Any {
        return CreateStepDefinitionIntentionAction()
    }
}