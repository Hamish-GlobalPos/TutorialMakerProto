pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TutorialMakerProto"

include(":dummyApp")
include(":tutorialmaker:player")
include(":tutorialmaker:editor")
