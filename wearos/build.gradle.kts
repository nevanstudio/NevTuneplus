// Copyright (c) 2026 Sachit. All rights reserved.
// Project: NevTune+
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

/*
 * Audify for Wear OS: a remote for the phone app, not a second player.
 *
 * It holds no library, streams nothing and runs no Palette or blur of its own —
 * the phone sends it what is playing, the cover as a 240px JPEG, and the mesh
 * colours the phone's player already computed (see WearBridge in :app). Which is
 * what keeps it small and smooth on a Galaxy Watch 4 class watch.
 *
 * It has to share the phone app's application id *and* signing key, because the
 * Wearable Data Layer only delivers between apps that match on both. So:
 *   dev  -> com.dev.nevtuneplus   pairs only with the phone's dev build
 *   prod -> com.nevanstudio.nevtuneplus pairs only with the phone's prod build
 * and each build type is signed exactly the way the phone's is (debug key for
 * debug, the release keystore for release), so a watch build pairs with the
 * phone build of the same variant.
 */

val signing = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
    val extFile = rootProject.file("../keystore_nevtunes/keystore.properties")
    if (extFile.exists()) {
        val extProps = Properties().apply { extFile.inputStream().use { load(it) } }
        extProps.forEach { (k, v) -> putIfAbsent(k, v) }
    }
}

val betaSuffix = rootProject.extra["audifyBetaSuffix"] as String

android {
    namespace = "com.nevanstudio.nevtuneplus.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nevanstudio.nevtuneplus"
        // Wear OS 3, the oldest a Galaxy Watch 4 ever shipped with.
        minSdk = 30
        targetSdk = 36
        // The phone's version — see the root build.gradle.kts.
        versionCode = rootProject.extra["audifyVersionCode"] as Int
        versionName = rootProject.extra["audifyVersionName"] as String
    }

    // Keep the same universal + ABI-specific release outputs as phone and TV.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    sourceSets {
        // The phone <-> watch protocol, shared with :app. See WearProtocol.
        getByName("main") { kotlin.directories += "protocol" }
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationId = "com.dev.nevtuneplus"
            resValue("string", "app_name", "NevTune+ Dev")
        }
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "NevTune+")
        }
    }

    signingConfigs {
        val storePath = signing.getProperty("storeFile")
            ?: signing.getProperty("KEYSTORE_PATH")
            ?: signing.getProperty("KEYSTORE")
            ?: System.getenv("KEYSTORE_PATH")
            ?: System.getenv("KEYSTORE")
            ?: "../keystore_nevtunes/nevtuneplus_key"

        val store = rootProject.file(storePath).takeIf { it.exists() }
            ?: file(storePath).takeIf { it.exists() }

        val storePass = signing.getProperty("storePassword")
            ?: signing.getProperty("KEYSTORE_PASSWORD")
            ?: System.getenv("KEYSTORE_PASSWORD")
        val alias = signing.getProperty("keyAlias")
            ?: signing.getProperty("KEY_ALIAS")
            ?: System.getenv("KEY_ALIAS")
        val keyPass = signing.getProperty("keyPassword")
            ?: signing.getProperty("KEY_PASSWORD")
            ?: System.getenv("KEY_PASSWORD")

        if (store != null && !storePass.isNullOrEmpty() && !alias.isNullOrEmpty() && !keyPass.isNullOrEmpty()) {
            create("release") {
                storeFile = store
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        /*
         * Debug is shrunk and non-debuggable too, unlike the phone's. It still
         * signs with the debug key — that is the part pairing depends on — but a
         * debuggable, un-R8'd Compose app is the slow kind, and on a watch CPU
         * that is the difference between a smooth crown and a stuttering one.
         * Pass -Paudify.wearDebuggable=true to get a debugger back.
         */
        debug {
            if (betaSuffix.isNotEmpty()) versionNameSuffix = "-$betaSuffix"
            val debuggable = providers.gradleProperty("nevtuneplus.wearDebuggable").orNull == "true"
            isDebuggable = debuggable
            isMinifyEnabled = !debuggable
            isShrinkResources = !debuggable
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            if (betaSuffix.isNotEmpty()) versionNameSuffix = "-$betaSuffix"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Null without a keystore, like the phone: an unsigned APK rather
            // than a build failure.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        resValues = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin/**",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // Media3's custom DataSource and session APIs are all marked unstable.
        optIn.add("androidx.media3.common.util.UnstableApi")
    }
}

// Named after the phone APKs (Audify-v1.8.apk) but with their own prefix, so
// the phone's updater — which only takes assets starting "Audify-" — can never
// pick a watch APK by mistake.
base.archivesName.set("NevTunePlusWear")

dependencies {
    implementation("androidx.wear.compose:compose-material3:1.7.1")
    implementation("androidx.wear.compose:compose-foundation:1.7.1")
    implementation("androidx.wear.compose:compose-navigation:1.7.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    // AmbientLifecycleObserver: the always-on player.
    implementation("androidx.wear:wear:1.4.0")
    // NowPlayingListener: our icon on the watch face, opening our player.
    implementation("androidx.wear:wear-ongoing:1.1.0")

    // The watch as the speaker: a player reading from the phone, in a session
    // so it plays on with the screen off. Same Media3 as the phone app.
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")

    // The Data Layer: state from the phone, commands to it.
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Lets a sideloaded install use the baseline profiles Compose ships with,
    // instead of starting every screen interpreted.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
}
