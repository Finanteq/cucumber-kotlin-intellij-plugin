plugins {
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    id("maven-publish")
}

group = "com.finanteq.plugins.idea"
version = "242.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.finanteq.plugins.idea.cucumber-kotlin-idea-plugin"
        name = "Cucumber for Kotlin and Android"
        version.set(project.version.toString())

        ideaVersion {
            untilBuild = provider { null }
        }

    }
    buildSearchableOptions.set(false)

    signing {
        certificateChainFile.set(file(findProperty("finanteqOpenSourceCertFile") as String))
        privateKeyFile.set(file(findProperty("finanteqOpenSourceKeyFile") as String))
        password.set(findProperty("finanteqOpenSourceKeyPassword") as String)
    }

    publishing {
        token.set(findProperty("finanteqOpenSourceJetbrainsPublishToken") as String)
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2.1")

        plugin("gherkin:242.20224.159")
        plugin("cucumber-java:242.20224.159")
        plugin("org.jetbrains.android:242.23339.11")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}