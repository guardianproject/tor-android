// Top-level build file where you can add configuration options common to all subprojects
val versionCode = 49080
val versionName = "0.4.9.8"
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
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

extra.apply {
    set("versionCode", versionCode)
    set("versionName", versionName)
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
