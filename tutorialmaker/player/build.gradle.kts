import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = "com.pano.tutorialmaker"
version = "0.4.10"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        publishLibraryVariants("release")
    }
    jvm("desktop")

    // iOS targets — only compiled on macOS; publish via GitHub Actions (macos runner)
    if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            // Explicit version, not the CMP-plugin default — see the comment in
            // tutorialmaker/editor/build.gradle.kts for why this must track the app's version.
            implementation(libs.compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okio)
        }
    }
}

android {
    namespace = "com.pano.tutorialmaker.player"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Hamish-GlobalPos/TutorialMakerProto")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
