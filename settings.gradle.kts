pluginManagement {
    repositories {
        google()
        mavenCentral()
        // used for foojay resolver, should be last
        //developer.android.com/build/optimize-your-build#gradle_plugin_portal
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // or PREFER_PROJECT
    repositories {
        google()
        mavenCentral()
    }
}

include(":tor-android-binary", ":sampletorapp")
