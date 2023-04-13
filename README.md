# Cucumber for Kotlin and Android Intellij Plugin

Plugin for Intellij IDEA and Android Studio which helps writing and implementing [Cucumber](https://cucumber.io/) tests in [Kotlin](https://kotlinlang.org/).

It's built on top of Cucumber for Java Intellij plugin.

## Plugin features

##### Navigation between step in Gherkin file and its implementation in Kotlin

##### Navigation between Kotlin step implementation and step usages in Gherkin files

![navigation_frm_impl_to_steps.png](docs/images/navigation_frm_impl_to_steps.png)

##### Presenting suggestions for typed gherkin step.

Existing matching steps are picked from all steps (implemented and not yet implemented while Cucumber for Java plugin suggests only implemented steps)

![step_suggestions.png](docs/images/step_suggestions.png)

##### Creating Kotlin step implementation

Either from Quick Fix action (for not implemented step only)

![create_step_quick_fix.png](docs/images/create_step_quick_fix.png)

or from always available Intention Action (for any step)

![create_step_intention_action.png](docs/images/create_step_intention_action.png)

Step can be added to existing Kotlin class (which already has some step inside) or to newly created file

![create_new_steps_file.png](docs/images/create_new_steps_file.png)

##### Creating Kotlin data class representing Gherkin data table

For normal, horizontal table just invoke intent actions in the table headers row line

![generate_table_class_action.png](docs/images/generate_table_class_action.png)

![generate_table_class_popup.png](docs/images/generate_table_class_popup.png)

For vertical/transposed table:

- select column which contains header names
- invoke intent actions on this selection

![generate_transposed_table_action.png](docs/images/generate_transposed_table_action.png)

##### Running specific example from `Scenario Outline`

![running_example.png](docs/images/running_example.png)

## Android specific features

##### Running scenario or example as Android Instrumented Test directly from Gherkin file

This requires using [cucumber-android](https://github.com/cucumber/cucumber-android) as `androidTest***` dependency

Run single scenario

![run_android_scenario.png](docs/images/run_android_scenario.png)

Run single Scenario Outline example

![run_android_example.png](docs/images/run_android_example.png)

Run entire Feature

![run_android_feature.png](docs/images/run_android_feature.png)

Run all features in particular folder

![run_android_folder.png](docs/images/run_android_folder.png)

## Troubleshooting

1. Sometimes when you try to run Scenario on Android it picks wrong Run configuration type which is Cucumber Java. Then test fails because it tries to run it using Cucumber Java CLI. Configuration type should be Android Instrumented Tests. If this happens do the following:

- check icon of configuration, if there is ![cucumber_java_icon.png](docs/images/cucumber_java_icon.png) it means that it's wrong one
- it should look like this ![android_run_configuration.png](docs/images/android_run_configuration.png) (with green Android robot)
- remove wrongly created configuration
- refresh Gradle project
- run scenario again, it should detect that this is Android project and pick proper configuration type

## Building plugin

Right now this plugin is not published in JetBrains Marketplace so to use it you have to build it manually by invoking gradle task `buildPlugin`. It will build zip in `build/distributions` folder which can be installed in Intellij/Android Studio using "Install plugin from disk" option