import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Applique google-services uniquement si le fichier json existe pour ne pas bloquer la compilation initiale
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Lecture sécurisée des propriétés de signature depuis local.properties (Slide 15 du TP)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
fun prop(key: String, default: String = ""): String = localProps.getProperty(key, default)

android {
    namespace = "corsica.universita.collecbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "corsica.universita.collecbox"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Configuration pour la vérification App Links du domaine PWA
        manifestPlaceholders["pwaHost"] = "collecbox.netlify.app"
    }

    signingConfigs {
        create("release") {
            if (prop("RELEASE_STORE_FILE").isNotEmpty()) {
                storeFile = rootProject.file(prop("RELEASE_STORE_FILE"))
                storePassword = prop("RELEASE_STORE_PASSWORD")
                keyAlias = prop("RELEASE_KEY_ALIAS")
                keyPassword = prop("RELEASE_KEY_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Jetpack Compose & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.11.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity & Navigation & Lifecycle Compose
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Room Database (Local-First)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore (Preferences remplaçant localStorage)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (Outbox en tâche de fond)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Retrofit & OkHttp (Synchronisation réseau)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase (BOM pour aligner FCM et Analytics)
    val firebaseBom = platform("com.google.firebase:firebase-bom:33.6.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Fonctionnalité Native 1 : Widget Glance d'écran d'accueil
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Fonctionnalité Native 2 : CameraX + ML Kit Scanner de codes-barres on-device
    val cameraVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

