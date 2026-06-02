val versionCode : Int = 49090
val versionName : String = "0.4.9.9"

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
