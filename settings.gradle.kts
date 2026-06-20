pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master"))
    }
}

include(":tor-android-binary", ":sampletorapp")

// fail any new changes that will break in gradle 10's mandated configuration cache
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
