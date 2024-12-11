plugins {
    id("org.jetbrains.intellij.platform") version "2.2.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("maven-publish")
}

group = "com.finanteq.plugins.idea"
version = "243.1"

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

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.1")

        plugin("gherkin:243.22562.13")
        plugin("cucumber-java:243.22562.13")
        plugin("org.jetbrains.android:243.22562.145")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }
}