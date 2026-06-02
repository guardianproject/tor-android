import com.android.build.api.dsl.ApplicationExtension

plugins { alias(libs.plugins.android.application) }
configure<ApplicationExtension> {
    namespace = "org.torproject.android.sample"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = namespace
        minSdk = 24
        targetSdk = 37
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}

dependencies {
    implementation(libs.tor.android)
    implementation(libs.jtorctl)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.localbroadcast)
}
