// Top-level build file where you can add configuration options common to all subprojects

@file:Suppress("PropertyName")

val VERSION_CODE = 49050
val VERSION_NAME = "0.4.9.5"

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.agp)
    }
}
plugins {
    alias(libs.plugins.nmcp.aggregation)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

extra.apply {
    set("versionCode", VERSION_CODE)
    set("versionName", VERSION_NAME)
}

nmcpAggregation {
    centralPortal {
        // Safe way to access these, returns null if not found
        username = project.findProperty("sonatype.user") as String?
        password = project.findProperty("sonatype.token") as String?

        // publish manually from the portal
        //publishingType = "USER_MANAGED"
        // or if you want to publish automatically
        publishingType = "AUTOMATIC"
    }

    publishAllProjectsProbablyBreakingProjectIsolation()
}
