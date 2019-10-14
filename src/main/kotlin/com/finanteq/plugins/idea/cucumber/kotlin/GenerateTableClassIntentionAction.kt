package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.codeInsight.daemon.impl.quickfix.CreateClassKind
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.CreateClassDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import cucumber.runtime.table.CamelCaseStringConverter
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.plugins.cucumber.psi.GherkinFileType

class GenerateTableClassIntentionAction : IntentionAction {
    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = "gherkin"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.fileType != GherkinFileType.INSTANCE) return false

        val lineText = editor.getLineText()

        if (!lineText.trimStart().startsWith('|')) return false

        return true
    }

    private fun Editor.getLineText() = document.getText(TextRange(caretModel.visualLineStart, caretModel.visualLineEnd))

    override fun getText(): String = "Generate table class"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {

        val camelCaseStringConverter = CamelCaseStringConverter()
        val columns = editor.getLineText().trim().split('|').map { it.trim() }.filter { it.isNotEmpty() }.map { camelCaseStringConverter.map(it) }

        val dialog = CreateClassDialog(project, text, "", "", CreateClassKind.CLASS, true, null)
        val get = dialog.showAndGet()

        if (get) {
            ApplicationManager.getApplication().runWriteAction(Computable {
                val factory = KtPsiFactory(project)
                val className = dialog.className
                val kotlinFile = createKotlinFile("$className.kt", dialog.targetDirectory)
                kotlinFile.add(factory.createNewLine(3))
                val clazz = factory.createClass("data class $className")
                val constructor = factory.createPrimaryConstructor("")
                val constructorBody = constructor.valueParameterList!!
                columns.forEachIndexed { index, s ->
                    val property = factory.createProperty(s, "String", false)
                    constructorBody.addBefore(property, constructorBody.rightParenthesis)
                    if (index < columns.size - 1) {
                        constructorBody.addBefore(factory.createComma(), constructorBody.rightParenthesis)
                    }
                    constructorBody.addBefore(factory.createNewLine(), constructorBody.rightParenthesis)
                }
                clazz.add(constructor)
                kotlinFile.add(clazz)
                return@Computable kotlinFile
            }).navigate(false)
        }
    }
}

class GenerateTableClassIntentionActionFactory : ExtensionFactory {
    override fun createInstance(factoryArgument: String, implementationClass: String?): Any {
        return GenerateTableClassIntentionAction()
    }
}