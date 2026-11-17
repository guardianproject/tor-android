import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    signing
    id("maven-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

val versionCode : Int = 49170
val versionName : String = "0.4.9.17"

group = "info.guardianproject"

fun getVersionNameFromGitTag(): Provider<String> = providers.exec {
    commandLine("git", "describe", "--tags", "--always")
}.standardOutput.asText.map { it.trim() }

configure<LibraryExtension> {
    namespace = "org.torproject.jni"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    defaultConfig {
        minSdk = 24

        buildConfigField("int", "VERSION_CODE", versionCode.toString())
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.so"))))
    api(libs.androidx.localbroadcast)
}

tasks.register<Jar>("sourcesJar") {
    description = "Create jar file with sources for TorService.java"
    archiveBaseName.set("tor-android-${getVersionNameFromGitTag().get()}")
    archiveClassifier.set("sources")
    from("src/main/java", "src/main/kotlin")
}

tasks.dokkaGeneratePublicationJavadoc.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
}

tasks.register<Jar>("javadocJar") {
    description = "Create Javadoc file for TorService.java's documentation"
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    archiveBaseName.set("tor-android-${getVersionNameFromGitTag().get()}")
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

afterEvaluate {
    publishing {
        publications {
            val libraryGroup = "info.guardianproject"
            val libraryArtifactId = "tor-android"
            val libraryUrl = "https://github.com/guardianproject/tor-android"
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = libraryGroup
                artifactId = libraryArtifactId
                version = versionName

                pom {
                    name.set("TorAndroid")
                    description.set("Tor for Android")
                    url.set(libraryUrl)
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
                        url.set(libraryUrl)
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
