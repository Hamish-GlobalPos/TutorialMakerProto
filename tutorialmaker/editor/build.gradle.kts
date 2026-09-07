import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = "com.pano.tutorialmaker"
version = "0.4.8"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        publishLibraryVariants("release")
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            // Explicit version, not the CMP-plugin default — must match the consuming app's
            // material3 exactly (GuideMateRtk pins libs.compose.material3 the same way) or
            // calls compiled here against one binary shape of e.g. ExposedDropdownMenuBox can
            // NoSuchMethodError at runtime against a differently-shaped one on the classpath.
            implementation(libs.compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            // Switching material3 above to an explicit version dropped the icon artifact that
            // used to come in transitively via the CMP-plugin's compose.material3 accessor.
            implementation(compose.materialIconsExtended)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.okio)
            api(project(":tutorialmaker:player"))
        }
    }
}

android {
    namespace = "com.pano.tutorialmaker.editor"
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
