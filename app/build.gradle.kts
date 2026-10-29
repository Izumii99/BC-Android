plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.izumii.bcandroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.izumii.bcandroid"
        minSdk = 26
        targetSdk = 34
        versionCode = 16
        versionName = "2.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "mode"
    productFlavors {
        create("standard") {
            dimension = "mode"
            manifestPlaceholders["appName"] = "Bondage Club"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["roundIcon"] = "@mipmap/ic_launcher_round"
        }
        create("discreet") {
            dimension = "mode"
            applicationIdSuffix = ".discreet"
            manifestPlaceholders["appName"] = "BC"
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_transparent"
            manifestPlaceholders["roundIcon"] = "@mipmap/ic_launcher_transparent"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("signing/release.keystore")
            val releasePass = System.getenv("BC_RELEASE_KEY") ?: ("bc_" + "release_key")
            storePassword = releasePass
            keyAlias = "bc_release"
            keyPassword = releasePass
        }
    }




    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

tasks.register<Copy>("syncAddons") {
    description = "Syncs addons from upstream BC-Desktop Scripts directory to Android assets/addons"
    val candidate1 = rootProject.file("../Bondage Club/Scripts")
    val candidate2 = rootProject.file("../../Bondage Club/Scripts")
    val sourceDir = when {
        candidate1.exists() && candidate1.isDirectory -> candidate1
        candidate2.exists() && candidate2.isDirectory -> candidate2
        else -> null
    }
    if (sourceDir != null) {
        from(sourceDir)
        into(file("src/main/assets/addons"))
    }
}

tasks.matching { it.name == "preBuild" || (it.name.startsWith("merge") && it.name.endsWith("Assets")) }.configureEach {
    dependsOn("syncAddons")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
