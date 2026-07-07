plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.chromium.chrome.browser.satellitespace"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "../chrome/android/java/src/org/chromium/chrome/browser/satellitespace"
            )
            res.srcDirs(
                "../chrome/android/res"
            )
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(project(":chromium-stubs"))
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.11.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
}
