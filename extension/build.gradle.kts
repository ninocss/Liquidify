plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "dev.nino.squareglass"
    compileSdk = 36
    defaultConfig { minSdk = 33 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    compileOnly("io.reactivex.rxjava2:rxjava:2.2.21")
    compileOnly("androidx.media:media:1.7.0")
    compileOnly(project(":host-stubs"))
    compileOnly("androidx.appcompat:appcompat:1.7.1")
    // Compile against the exact versions embedded in TIDAL 2.200.0-10004.
    // Dependencies are deliberately NOT bundled with the extension.
    compileOnly("androidx.compose.runtime:runtime-android:1.10.2")
    compileOnly("androidx.compose.ui:ui-android:1.9.2")
    compileOnly("androidx.compose.foundation:foundation-android:1.9.2")
    compileOnly("androidx.compose.material3:material3-android:1.3.2")
    compileOnly("androidx.lifecycle:lifecycle-runtime-compose-android:2.10.0")
    compileOnly("androidx.recyclerview:recyclerview:1.4.0")
    compileOnly("androidx.constraintlayout:constraintlayout:2.2.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
