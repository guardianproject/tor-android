import com.android.build.api.dsl.ApplicationExtension

plugins { alias(libs.plugins.android.application) }

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

configure<ApplicationExtension> {
    namespace = "org.torproject.android.sample"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    defaultConfig {
        applicationId = namespace
        minSdk = 24
        targetSdk = 37
    }
}

dependencies {
    implementation(libs.tor.android)
    implementation(libs.jtorctl)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.localbroadcast)
}
