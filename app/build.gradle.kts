plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")

    id("dagger.hilt.android.plugin")
    id("com.google.dagger.hilt.android") // 👈 Plugin de Hilt

    id("androidx.navigation.safeargs.kotlin")

    id("com.squareup.sqldelight")

    id("kotlin-parcelize")

}

android {
    namespace = "com.ursolgleb.controlparental"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ursolgleb.controlparental"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keyControl.jks") // Ruta a tu keystore
            storePassword = "646807"
            keyAlias = "keyControlParental"
            keyPassword = "646807"
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
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}



dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.jsoup)
    implementation(libs.json)  // Lo mismo aquí
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.glide) // Última versión de Glide
    ksp(libs.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)


    // WorkManager con Hilt (opcional si usas WorkManager)
    implementation("androidx.hilt:hilt-work:1.0.0")
    ksp("androidx.hilt:hilt-compiler:1.0.0")



    implementation("com.google.dagger:hilt-android:2.49")
    ksp("com.google.dagger:hilt-compiler:2.49")

    val work_version = "2.9.1"

    // (Java only)
    implementation("androidx.work:work-runtime:$work_version")

    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // optional - RxJava2 support
    implementation("androidx.work:work-rxjava2:$work_version")

    // optional - GCMNetworkManager support
    implementation("androidx.work:work-gcm:$work_version")

    // optional - Test helpers
    androidTestImplementation("androidx.work:work-testing:$work_version")

    // optional - Multiprocess support
    implementation("androidx.work:work-multiprocess:$work_version")

    // Dependencias de Moshi añadidas más arriba; Gson ya no es necesario
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.cache4k)

    implementation(libs.sqlDelightAndroidDriver)
    implementation(libs.sqlDelightCoroutines)

    implementation("androidx.biometric:biometric:1.1.0")


    implementation("androidx.security:security-crypto:1.1.0-alpha06")


    implementation(libs.retrofit)
    // Moshi se usa como converter; dependencia Gson eliminada

    implementation(libs.androidx.emoji2.bundled)


    // Reemplazar Gson con Moshi
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    // Interceptor para depuración
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Se eliminó moshi-kotlin-codegen: usaremos reflexión para los adaptadores

}


