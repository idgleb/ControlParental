// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Hilt
    alias(libs.plugins.hilt) apply false

    // KSP (Kotlin Symbol Processing) - Alineado con la versión de Kotlin
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false

    kotlin("kapt") version "1.9.0" apply false
    id("com.android.library") version "8.1.4" apply false

    id("jp.ntsk.room-schema-docs") version "1.1.0"  // 👈 plugin del diagrama

}

roomSchemaDocs {
    schemaDir = "$projectDir/app/schemas"
    outputDir = "$projectDir/app/schemas-docs"
    // en terminal: ./gradlew generateRoomSchemaDocs
}


buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()    // ← imprescindible
    }
    dependencies {
        classpath (libs.gradle)
        classpath (libs.kotlin.gradle.plugin)
        classpath (libs.hilt.android.gradle.plugin)
        classpath (libs.androidx.navigation.safe.args.gradle.plugin)
        classpath (libs.sqlDelightGradle)
    }
}

