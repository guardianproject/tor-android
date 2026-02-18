plugins {
    id("com.android.application")
}
android {
    namespace = "org.torproject.android.sample"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "org.torproject.android.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("int", "VERSION_CODE", "${rootProject.extra["versionCode"]}")
        buildConfigField("String", "VERSION_NAME", "\"${rootProject.extra["versionName"]}\"")
    }
    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/com.android.tools/proguard/coroutines.pro",
                "META-INF/com.android.tools/proguard/coroutines.pro"
            )
        }
    }


    buildTypes {
        debug {
        }

        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    lint {
        abortOnError = false
    }

}

repositories {
    maven(uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master"))
}

dependencies {
    // tor-android and jtorctl (apps based off this sample don't need the noinspection comments)
    //noinspection UseTomlInstead
    implementation("info.guardianproject:tor-android:0.4.9.5")
    //noinspection UseTomlInstead
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    // to use a locally built .AAR of tor-android, replace the above with:
    //implementation (name:"tor-android-binary-release",ext:"aar")

    // other android dependencies:
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.localbroadcast)
}
