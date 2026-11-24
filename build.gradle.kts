// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // AGP 9 provides built-in Kotlin; pin its compiler toolchain to the
        // same fully supported Kotlin version used by the KMP modules.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.3.1" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
}

/*
 * Calendar Versioning (CalVer: yy.MM.dd) for release builds; debug builds stay
 * pinned to "1.0" (versionCode 1) so daily clock rollovers never invalidate
 * Android Studio's build cache or slow down local testing.
 *
 * Phone, TV, and Watch OS all read these values so their versions stay locked
 * together. No "v" in the name — the in-app updater parses it numerically.
 */
val isReleaseBuild =
    providers.gradleProperty("nevtune.release").orNull == "true" ||
        gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

val todayUtc = LocalDate.now(ZoneOffset.UTC)
val calVerName = todayUtc.format(DateTimeFormatter.ofPattern("yy.MM.dd"))
val calVerCode = todayUtc.format(DateTimeFormatter.ofPattern("yyMMdd")).toInt() * 100

val releaseVersionName = providers.gradleProperty("nevtuneVersionName").orNull ?: calVerName
val releaseVersionCode = providers.gradleProperty("nevtuneVersionCode").orNull?.toIntOrNull() ?: calVerCode

extra["nevtuneVersionCode"] = if (isReleaseBuild) releaseVersionCode else 1
extra["nevtuneVersionName"] = if (isReleaseBuild) releaseVersionName else "1.0"
extra["nevtuneBetaSuffix"] = ""
