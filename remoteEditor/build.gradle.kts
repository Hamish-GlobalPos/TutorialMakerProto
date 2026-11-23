import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.voyager.screenmodel)
                implementation(project(":tutorialmaker:player"))
                implementation(project(":tutorialmaker:editor"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.pano.tutorialmaker.remoteeditor.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "TutorialRemoteEditor"
            packageVersion = "1.0.0"
        }
    }
}
