package com.finanteq.plugins.idea.cucumber.kotlin

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import cucumber.api.PendingException
import cucumber.runtime.snippets.CamelCaseConcatenator
import cucumber.runtime.snippets.FunctionNameGenerator
import cucumber.runtime.snippets.SnippetGenerator
import gherkin.formatter.model.DataTableRow
import gherkin.formatter.model.Step
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.quickfix.moveCaretToEnd
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.plugins.cucumber.java.steps.AnnotationPackageProvider
import org.jetbrains.plugins.cucumber.java.steps.JavaStepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinStep
import java.util.*

class KotlinStepDefinitionCreator : JavaStepDefinitionCreator() {

    override fun createStepDefinition(step: GherkinStep, file: PsiFile, withTemplate: Boolean): Boolean {

        val ktFile = when (file) {
            is KtFile -> file
            is FakeFileForLightClass -> file.ktFile
            else -> return false
        }

        val project = file.project

        closeActiveTemplateBuilders(file)
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        val clazz = (ktFile.classes[0] as KtLightClass).kotlinOrigin!!

        val factory = KtPsiFactory(project)

        val classBody = clazz.getBody()!!
        val element = buildStepDefinitionByStep(step, factory)
        var addedElement = classBody.addBefore(element, classBody.rBrace) as KtNamedFunction
        addedElement = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(addedElement)

        addedElement.annotationEntries.forEach {
            val typeReference = it.typeReference!!
            val pathStr = typeReference.text
            val importPath = ImportPath.fromString(pathStr)
            addImport(ktFile, importPath, factory)
            (typeReference.typeElement as KtUserType).deleteQualifier()
        }
        addImport(ktFile, ImportPath.fromString(PendingException::class.java.name!!), factory)
        val editor = addedElement.findExistingEditor()
        addedElement.moveCaretToEnd(editor, project)
        editor?.scrollingModel?.scrollToCaret(ScrollType.MAKE_VISIBLE)
        return true
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
        val generator = SnippetGenerator(KotlinSnippet())

        val snippet = generator.getSnippet(cucumberStep, FunctionNameGenerator(CamelCaseConcatenator()))
                .replaceFirst("@".toRegex(), methodAnnotation)
                .replace("\\\\\\\\".toRegex(), "\\\\")
                .replace("\\\\d".toRegex(), "\\\\\\\\d")


        return factory.createFunction(snippet)
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

}