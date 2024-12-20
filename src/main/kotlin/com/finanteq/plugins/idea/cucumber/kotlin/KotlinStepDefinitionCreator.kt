package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import cucumber.runtime.snippets.CamelCaseConcatenator
import cucumber.runtime.snippets.FunctionNameGenerator
import cucumber.runtime.snippets.SnippetGenerator
import gherkin.formatter.model.DataTableRow
import gherkin.formatter.model.Step
import io.cucumber.cucumberexpressions.CucumberExpressionGenerator
import io.cucumber.cucumberexpressions.ParameterTypeRegistry
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.plugins.cucumber.java.CucumberJavaUtil
import org.jetbrains.plugins.cucumber.java.config.CucumberConfigUtil
import org.jetbrains.plugins.cucumber.java.steps.AnnotationPackageProvider
import org.jetbrains.plugins.cucumber.java.steps.JavaStepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.util.*


class KotlinStepDefinitionCreator : JavaStepDefinitionCreator() {

    override fun createStepDefinition(step: GherkinStep, file: PsiFile, withTemplate: Boolean): Boolean {

        val ktFile = when (file) {
            is KtFile -> file
            is FakeFileForLightClass -> file.ktFile
            else -> return super.createStepDefinition(step, file, withTemplate)
        }

        val project = file.project

        closeActiveTemplateBuilders(file)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val clazz = (ktFile.classes[0] as KtLightClass).kotlinOrigin!!

        val factory = KtPsiFactory(project)

        val classBody = clazz.body!!
        val element = buildStepDefinitionByStep(step, factory)
        var addedElement = classBody.addBefore(element, classBody.rBrace) as KtNamedFunction
        addedElement = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(addedElement)!!

        addedElement.annotationEntries.forEach {
            val typeReference = it.typeReference!!
            val pathStr = typeReference.text
            val importPath = ImportPath.fromString(pathStr)
            addImport(ktFile, importPath, factory)
            (typeReference.typeElement as KtUserType).deleteQualifier()
        }
        addImport(ktFile, ImportPath.fromString(getPendingExceptionFqn(ktFile)), factory)
        val editor = addedElement.findExistingEditor()
        addedElement.moveCaretToEnd(editor, project)
        editor?.scrollingModel?.scrollToCaret(ScrollType.MAKE_VISIBLE)
        return true
    }

    private fun getPendingExceptionFqn(psiElement: PsiElement): String {
        val coreVersion = CucumberConfigUtil.getCucumberCoreVersion(psiElement)
            ?: return "cucumber.api.PendingException"
        return if (coreVersion >= "5.0") "io.cucumber.java.PendingException" else "cucumber.api.PendingException"
    }

    private fun addImport(ktFile: KtFile, importPath: ImportPath, factory: KtPsiFactory) {
        if (ktFile.importDirectives.none { it.importPath == importPath }) {
            ktFile.importList?.add(factory.createImportDirective(importPath))
        }
    }

    private fun buildStepDefinitionByStep(step: GherkinStep, factory: KtPsiFactory): KtNamedFunction {
        val annotationPackage = AnnotationPackageProvider().getAnnotationPackageFor(step)
        val methodAnnotation = String.format("@%s.", annotationPackage)

        val rows = step.table?.dataRows?.mapIndexed { index, row -> DataTableRow(emptyList(), row.psiCells.map { it.text }, index) }
        val cucumberStep = Step(ArrayList(), step.keyword.text, step.name, 0, rows, null)
        val generator = SnippetGenerator(KotlinSnippet)

        var snippet = generator.getSnippet(cucumberStep, FunctionNameGenerator(CamelCaseConcatenator()))
            .replaceFirst("@".toRegex(), methodAnnotation)
            .replace("\\\\\\\\".toRegex(), "\\\\")
            .replace("\\\\d".toRegex(), "\\\\\\\\d")

        if (CucumberJavaUtil.isCucumberExpressionsAvailable(step)) {
            snippet = replaceRegexpWithCucumberExpression(snippet, step.name)
        }

        return factory.createFunction(snippet)
    }


    private fun replaceRegexpWithCucumberExpression(snippet: String, step: String): String {
        try {
            val registry = ParameterTypeRegistry(Locale.getDefault())
            val generator = CucumberExpressionGenerator(registry)
            val result = generator.generateExpressions(step)[0]
            if (result != null) {
                val cucumberExpression = KotlinSnippet.escapePattern(result.source)
                val firstLineEnd = snippet.indexOf('\n')
                val firstLine = snippet.substring(0, firstLineEnd)
                val start = firstLine.indexOf('(') + 1
                val newFirstLine = firstLine.substring(0, start + 1) + cucumberExpression + "\")"
                return "$newFirstLine${snippet.substring(firstLineEnd)}"
            }
        } catch (ignored: Exception) {
            //ignored
        }
        return snippet
    }

    override fun createStepDefinitionContainer(dir: PsiDirectory, name: String): PsiFile {

        val project = dir.project
        return ApplicationManager.getApplication().runWriteAction(Computable {
            val factory = KtPsiFactory(project)
            val kotlinFile = createKotlinFile("$name.kt", dir)
            kotlinFile.add(factory.createNewLine(3))
            kotlinFile.add(factory.createClass("class $name {\n\n}"))
            return@Computable kotlinFile
        })

    }

    private fun PsiElement.moveCaretToEnd(editor: Editor?, project: Project) {
        editor?.run {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            val endOffset = if (text.endsWith(")")) endOffset - 1 else endOffset
            document.insertString(endOffset, " ")
            caretModel.moveToOffset(endOffset + 1)
        }
    }

}