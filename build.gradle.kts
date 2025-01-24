import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("maven-publish")
}

group = "com.finanteq.plugins.idea"
version = "242.2-androidStudio"

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

    pluginVerification {
        ides {
            ide(IntelliJPlatformType.AndroidStudio, "2024.2.2.1")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.2")
            ide(IntelliJPlatformType.IntellijIdeaUltimate, "2024.2.2")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        androidStudio("2024.2.2.1")
        plugin("gherkin:242.20224.159")
        plugin("cucumber-java:242.20224.159")
        plugin("org.jetbrains.android:243.22562.218")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")


        pluginVerifier()
        zipSigner()
    }
}