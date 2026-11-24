// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "nevtuneplus"
include(":shared")
include(":sharedUi")
include(":app")
include(":tv")
include(":wearos")
