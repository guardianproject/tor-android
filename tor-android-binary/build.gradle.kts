import java.io.ByteArrayOutputStream

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android").version("2.2.20")
    id("org.jetbrains.dokka").version("2.1.0")
    id("org.jetbrains.dokka-javadoc").version("2.1.0")
    id("maven-publish")
    id("signing")
}

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    api("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    api("info.guardianproject:jtorctl:0.4.5.7")

    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("info.guardianproject.netcipher:netcipher:2.1.0")
    androidTestImplementation("commons-io:commons-io:2.11.0")
    androidTestImplementation("commons-net:commons-net:3.6")
}

tasks {
    val sourcesJar by registering(Jar::class) {
        archiveBaseName.set("tor-android-" + getVersionName.standardOutput.asText.get().trim())
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }

    artifacts {
        archives(sourcesJar)
    }
}

tasks.dokkaGeneratePublicationJavadoc.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
}

tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
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


