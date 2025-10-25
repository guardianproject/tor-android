plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    id("maven-publish")
    id("signing")
}

kotlin { jvmToolchain(21) }

group = "info.guardianproject"

val getVersionName = providers.exec {
    commandLine("git", "describe", "--tags", "--always")
}

android {
    namespace = "org.torproject.jni"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testOptions.targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        // Replace with actual values or move to gradle.properties or version catalog
        buildConfigField("int", "VERSION_CODE", rootProject.extra["versionCode"].toString())
        buildConfigField("String", "VERSION_NAME", "\"${rootProject.extra["versionName"]}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += "META-INF/androidx.localbroadcastmanager_localbroadcastmanager.version"
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.so"))))
    api(libs.androidx.localbroadcast)
    api(libs.jtorctl)

    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.netcipher)
    androidTestImplementation(libs.commons.io)
    androidTestImplementation(libs.commons.net)
}

tasks.register<Jar>("sourcesJar") {
    archiveBaseName.set("tor-android-" + getVersionName.standardOutput.asText.get().trim())
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

tasks.dokkaGeneratePublicationJavadoc.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
}

tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    archiveBaseName.set("tor-android-" + getVersionName.standardOutput.asText.get().trim())
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = rootProject.extra["LIBRARY_GROUP"].toString()
                artifactId = rootProject.extra["LIBRARY_ARTIFACT_ID"].toString()
                version = rootProject.extra["versionName"].toString()

                pom {
                    name.set("TorAndroid")
                    description.set("Tor for Android")
                    url.set("https://github.com/guardianproject/tor-android")
                    licenses {
                        license {
                            name.set("The 3-Clause BSD License")
                            url.set("https://opensource.org/license/bsd-3-clause")
                        }
                    }
                    developers {
                        developer {
                            id.set("guardianproject")
                            name.set("Guardian Project")
                            email.set("nathan@guardianproject.info")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/guardianproject/tor-android.git")
                        developerConnection.set("scm:git:ssh://git@github.com:guardianproject/tor-android.git")
                        url.set("https://github.com/guardianproject/tor-android")
                    }
                }
            }
        }
	repositories {
    		maven {
        		name = "GitHubPackages"
        		url = uri("https://maven.pkg.github.com/guardianproject/tor-android")
        		credentials {
            			username = findProperty("gpr.user") as String?
            			password = findProperty("gpr.key") as String?
        		}
    		}
	}

    }


    signing {
        sign(publishing.publications["release"])
    }

}


