package com.finanteq.plugins.idea.cucumber.kotlin.run.android

import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.android.tools.idea.projectsystem.TestArtifactSearchScopes
import com.android.tools.idea.run.AndroidRunConfigurationType
import com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfiguration
import com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfigurationType
import com.finanteq.plugins.idea.cucumber.kotlin.run.isExampleInExamples
import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.execution.junit.JavaRunConfigurationProducerBase
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.elementType
import org.jetbrains.android.util.AndroidUtils
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.jetbrains.plugins.cucumber.psi.*
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType

class CucumberKotlinAndroidRunConfigurationProducer : JavaRunConfigurationProducerBase<AndroidTestRunConfiguration>() {


    override fun setupConfigurationFromContext(
        configuration: AndroidTestRunConfiguration,
        context: ConfigurationContext,
        sourceElementRef: Ref<PsiElement>
    ): Boolean {
        val configurator = AndroidTestConfigurator(context) ?: return false
        if (!configurator.configure(configuration, sourceElementRef)) {
            return false
        }

        // Set context.module to the configuration using the utility method from the base class. It may
        // set non-context module such as pre-defined module in configuration template.
        return setupConfigurationModule(context, configuration)
    }

    override fun isConfigurationFromContext(configuration: AndroidTestRunConfiguration, context: ConfigurationContext): Boolean {
        val expectedConfig = configurationFactory.createTemplateConfiguration(configuration.project) as AndroidTestRunConfiguration
        val configurator = AndroidTestConfigurator(context) ?: return false
        if (!configurator.configure(expectedConfig, Ref())) {
            return false
        }
        if (configuration.TESTING_TYPE != expectedConfig.TESTING_TYPE) {
            return false
        }

        return configuration.EXTRA_OPTIONS == expectedConfig.EXTRA_OPTIONS
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean = when {
        // This configuration producer works best for Gradle based project. If the configuration is generated
        // for non-Gradle project and other configuration is available, prefer the other one.
        !GradleProjectInfo.getInstance(self.configuration.project).isBuildWithGradle -> false

        // If the other configuration type is JUnitConfigurationType or GradleExternalTaskConfigurationType, prefer our configuration.
        // Although those tests may be able to run on both environment if they are written with the unified-api (androidx.test, Espresso),
        // here we prioritize instrumentation.
        other.configurationType is JUnitConfigurationType -> true
        other.configurationType is GradleExternalTaskConfigurationType -> true

        // Otherwise, we don't have preference. Let the IDE to decide which one to use.
        else -> false
    }

    override fun getConfigurationFactory(): ConfigurationFactory = AndroidTestRunConfigurationType.getInstance().factory
}

/**
 * A helper class responsible for configuring [AndroidTestRunConfiguration] properly based on given information.
 * This is a stateless class and you can call [configure] method as many times as you wish.
 */
private class AndroidTestConfigurator(
    private val module: Module,
    private val testScopes: TestArtifactSearchScopes,
    private val location: Location<PsiElement>,
    private val virtualFile: VirtualFile
) {
    companion object {
        /**
         * Creates [AndroidTestConfigurator] from a given context.
         * Returns null if the context is not applicable for android test.
         */
        operator fun invoke(context: ConfigurationContext): AndroidTestConfigurator? {
            val location = context.location ?: return null
            val module = AndroidUtils.getAndroidModule(context) ?: return null
            val testScopes = TestArtifactSearchScopes.getInstance(module) ?: return null
            val virtualFile = PsiUtilCore.getVirtualFile(location.psiElement) ?: return null
            return AndroidTestConfigurator(module, testScopes, location, virtualFile)
        }

    }

    /**
     * Configures a given configuration. If success, it returns true otherwise false.
     * When the configuration fails, the given configuration object may be configured in a halfway so you should dispose the
     * configuration.
     *
     * @param configuration a configuration instance to be configured
     * @param sourceElementRef the most relevant [PsiElement] such as test method, test class, or package is set back to the caller
     * for reference
     */
    fun configure(
        configuration: AndroidTestRunConfiguration,
        sourceElementRef: Ref<PsiElement>
    ): Boolean {
        if (!testScopes.isAndroidTestSource(virtualFile)) {
            return false
        }

        val targetSelectionMode = AndroidUtils.getDefaultTargetSelectionMode(
            module, AndroidTestRunConfigurationType.getInstance(), AndroidRunConfigurationType.getInstance()
        )
        if (targetSelectionMode != null) {
            configuration.deployTargetContext.targetSelectionMode = targetSelectionMode
        }

        // Try to create run configuration from the most specific one to the broader.
        return when {
            tryScenarioTestConfiguration(configuration, sourceElementRef) -> true
            tryScenarioOutlineExampleTestConfiguration(configuration, sourceElementRef) -> true
            tryFeatureTestConfiguration(configuration, sourceElementRef) -> true
            tryAllInFolderTestConfiguration(configuration, sourceElementRef) -> true
            else -> false
        }
    }

    /**
     * Tries to configure for a single scenario test. Returns true if success otherwise false.
     */
    private fun tryScenarioTestConfiguration(configuration: AndroidTestRunConfiguration, sourceElementRef: Ref<PsiElement>): Boolean {
        return tryGherkinEntity(
            configuration,
            sourceElementRef,
            isGherkinEntity = { GherkinTokenTypes.SCENARIOS_KEYWORDS.contains(location.psiElement.elementType) },
            suggestedName = { getScenarioName(it) },
            featuresPath = { getScenarioPath() }
        )
    }

    private fun tryScenarioOutlineExampleTestConfiguration(configuration: AndroidTestRunConfiguration, sourceElementRef: Ref<PsiElement>): Boolean {
        return tryGherkinEntity(
            configuration,
            sourceElementRef,
            isGherkinEntity = { location.psiElement.isExampleInExamples() },
            suggestedName = { findScenarioName()?.let { "$it, at ${getLineNumber()}" } ?: getFallbackScenarioName(it) },
            featuresPath = { getScenarioPath() }
        )
    }

    private fun getScenarioName(path: String): String {
        return findScenarioName() ?: getFallbackScenarioName(path)
    }

    private fun getFallbackScenarioName(path: String) = "Scenario at $path"

    private fun findScenarioName(): String? {
        val sourceElement = location.psiElement
        val scenario = PsiTreeUtil.getParentOfType(sourceElement, GherkinScenario::class.java, GherkinScenarioOutline::class.java)
        return scenario?.scenarioName
    }

    private fun getScenarioPath(): String {
        return "${getFeaturePath()}:${getLineNumber()}"
    }

    private fun getLineNumber(): Int {
        val sourceElement = location.psiElement
        return sourceElement.getLineNumber() + 1
    }

    /**
     * Tries to configure for a single feature test. Returns true if success otherwise false.
     */
    private fun tryFeatureTestConfiguration(configuration: AndroidTestRunConfiguration, sourceElementRef: Ref<PsiElement>): Boolean {
        return tryGherkinEntity(
            configuration,
            sourceElementRef,
            isGherkinEntity = { location.psiElement.elementType == GherkinTokenTypes.FEATURE_KEYWORD || location.psiElement is GherkinFile },
            suggestedName = { getFeatureName(it) },
            featuresPath = { getFeaturePath() }
        )
    }

    private fun getFeatureName(path: String): String {
        val sourceElement = location.psiElement
        val feature = PsiTreeUtil.getParentOfType(sourceElement, GherkinFeature::class.java)
        return feature?.featureName ?: "Feature in $path"
    }

    private fun getFeaturePath(): String {
        val sourceElement = location.psiElement
        val containingFile = sourceElement.containingFile
        return containingFile.relativePathInSourceRoot()
    }

    private fun PsiFileSystemItem.relativePathInSourceRoot() = virtualFile.path.removePrefix(sourceRoot?.path.orEmpty()).removePrefix("/")


    private fun tryGherkinEntity(configuration: AndroidTestRunConfiguration, sourceElementRef: Ref<PsiElement>, isGherkinEntity: (IElementType?) -> Boolean, suggestedName: (path: String) -> String, featuresPath: () -> String): Boolean {
        if (isGherkinEntity(location.psiElement.elementType)) {
            setupConfiguration(sourceElementRef, configuration, suggestedName, featuresPath)
            return true
        }
        return false
    }

    private fun setupConfiguration(sourceElementRef: Ref<PsiElement>, configuration: AndroidTestRunConfiguration, suggestedName: (path: String) -> String, featuresPath: () -> String?) {
        sourceElementRef.set(location.psiElement)
        configuration.TESTING_TYPE = AndroidTestRunConfiguration.TEST_ALL_IN_MODULE
        val path = featuresPath()
        configuration.name = suggestedName(path.orEmpty())
        if (path != null) {
            configuration.EXTRA_OPTIONS = "-e features $path"
        }
    }

    /**
     * Tries to configure for an all-in-folder test. Returns true if success otherwise false.
     */
    private fun tryAllInFolderTestConfiguration(configuration: AndroidTestRunConfiguration, sourceElementRef: Ref<PsiElement>): Boolean {

        val locationFile = location.virtualFile
        locationFile ?: return false
        val project = configuration.project
        if (locationFile.isDirectory && locationFile.hasFeatureFiles(project)) {

            setupConfiguration(sourceElementRef, configuration,
                suggestedName = { "All Features in: $it" },
                featuresPath = { locationFile.toPsiDirectory(project)?.relativePathInSourceRoot() })
            return true
        }
        return false
    }

    private fun VirtualFile.hasFeatureFiles(project: Project): Boolean {
        return FileTypeIndex.containsFileOfType(GherkinFileType.INSTANCE, GlobalSearchScopesCore.directoryScope(project, this, true))
    }
}