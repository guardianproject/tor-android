import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
}

kotlin {
    jvmToolchain(21)
}

configure<ApplicationExtension> {
    namespace = "org.torproject.android.sample"
    compileSdk = 37
    defaultConfig {
        applicationId = namespace
        minSdk = 24
        targetSdk = 37
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

repositories {
    // this repository is needed for tor-android and jtorctl
    // normally this goes in the top-level build.gradle.kts, but keeping it here for simplicity
    maven(uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master"))
}

//noinspection UseTomlInstead
dependencies {
    // apps built with tor-android need to include as dependencies both tor-android + jtorctl:
    implementation("info.guardianproject:tor-android:0.4.9.6.2")
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    // other android dependencies:
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.localbroadcast)
}
