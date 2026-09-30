plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// If the project lives in a OneDrive folder, the build output is redirected outside it (OneDrive locks files and breaks the build).
// Elsewhere (a regular folder, CI, another computer) the standard app/build is used.
if (rootDir.path.contains("OneDrive", ignoreCase = true)) {
    val base = System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir")
    layout.buildDirectory.set(file("$base/A9-build/app"))
}

android {
    namespace = "lt.tbu.a9"
    compileSdk = 37

    defaultConfig {
        applicationId = "lt.tbu.a9"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release signing: credentials come from the user-level ~/.gradle/gradle.properties (A9_STORE_FILE, A9_STORE_PASSWORD,
    // A9_KEY_ALIAS, A9_KEY_PASSWORD), never from the repository. Without them the release build is signed with the debug key.
    val hasReleaseKey = providers.gradleProperty("A9_STORE_FILE").isPresent
    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(providers.gradleProperty("A9_STORE_FILE").get())
                storePassword = providers.gradleProperty("A9_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("A9_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("A9_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
