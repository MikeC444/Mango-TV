plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "tv.mango.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "tv.mango.app"

        // Fire OS 5 (API 22) through Fire OS 7 (API 28) and Android TV beyond it.
        // Nothing in the design needs a newer API, so we take the wider device net.
        minSdk = 22
        targetSdk = 34

        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables.useSupportLibrary = true

        // No touch, no landscape/portrait split, no RTL mirroring of artwork:
        // trimming unused resource configurations keeps the APK small.
        resourceConfigurations += listOf("en")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isPseudoLocalesEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Chosen over the alternatives for its bitmap pool and its willingness to
    // decode straight into RGB_565, which halves the memory a screen of
    // artwork occupies. On a 1 GB stick that is the difference that matters.
    //
    // Deliberately used without an AppGlideModule: the only settings this
    // application needs are per-request, so they live in ImageLoader rather
    // than costing an annotation processor on every build.
    implementation(libs.glide)

    // Watchlist membership and playback positions: a set of identifiers and a
    // map of identifier to position. DataStore covers that reactively in a
    // few hundred lines, where Room would cost an annotation processor on
    // every build for queries this data does not have. Room becomes the right
    // answer when the library grows real ones - sorting, filtering, joins.
    implementation(libs.androidx.datastore.preferences)

    // Add-ons are remote HTTP services. OkHttp gives per-call timeouts, proper
    // cancellation and connection reuse across the several add-ons a single
    // screen may query at once.
    implementation(libs.okhttp)

    // Renders the QR code on the Add Add-on screen. Pure Java, no transitive
    // Android dependencies of its own - it only draws a bitmap from text
    // already in hand, nothing here touches the network.
    implementation(libs.zxing.core)

    // Playback. HLS and DASH modules are separate from the core player
    // because most add-on streams are progressive MP4 and paying for two
    // manifest parsers on every install is not free; DefaultMediaSourceFactory
    // picks whichever a stream actually turns out to need.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
