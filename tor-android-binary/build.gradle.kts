import java.io.ByteArrayOutputStream

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
    id("io.deepmedia.tools.deployer") version "0.16.0"
}

fun getVersionName(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "describe", "--tags", "--always")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

android {
    namespace = "org.torproject.jni"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "info.guardianproject" // ← Your group ID
                artifactId = "tor-android"       // ← Your artifact ID
                version = "0.4.8.16"

                pom {
                    name.set("TorAndroid")
                    description.set("Tor for Android")
                    url.set("https://github.com/guardianproject/tor-android")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
            			username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            			password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        		}
    		}
		// Sonatype OSSRH
            maven {
                name = "ossrh-staging-api"
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.findProperty("ossrh.user") as String
                    password = project.findProperty("ossrh.pass") as String
                }
            }
	}

    }

    signing {
        useGpgCmd()
        sign(publishing.publications["release"])
    }
}


deployer {

content {
    androidComponents("release", "merged") {
        // Optional configuration, invoked on each component.
    }
}

// In the deployer{} block, or within a spec declaration...
projectInfo {
   // Project name. Defaults to rootProject.name
   name.set("tor-android")
   // Project description. Defaults to rootProject.name
   description.set("tor for android")
   // Project url
   url.set("https://github.com/guardianproject/tor-android")
   // Package group id. Defaults to project's group
   groupId.set("info.guardianproject")
   // Package artifact. Defaults to project's archivesName or project.name
   artifactId.set("tor-android")
   // Project SCM information. Defaults to project.url
   scm {
       // or: fromGithub("deepmedia", "MavenDeployer")
       // or: fromBitbucket("deepmedia", "MavenDeployer")
       // or: set url, connection and developerConnection directly
   }
   // Licenses. Apache 2.0 and MIT are built-in
   license(MIT)
   // Developers
   developer("guardianproject", "nathan@guardianproject.info")
}

    localSpec {
        directory.set(file("/tmp/tor-android"))
    }

// Common configuration...

    centralPortalSpec {
        // Take these credentials from the Generate User Token page at https://central.sonatype.com/account
        auth.user.set(secret("ossrh.user"))
        auth.password.set(secret("ossrh.pass"))

        // Signing is required
        signing.key.set(secret("signing.keyId"))
        signing.password.set(secret("signing.passphrase"))
       allowMavenCentralSync = false

    }


}
