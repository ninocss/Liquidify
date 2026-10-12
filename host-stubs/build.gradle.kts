plugins { `java-library` }
dependencies {
    compileOnly(files("${System.getenv("ANDROID_HOME") ?: "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"}/platforms/android-36/android.jar"))
}
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
    compileOnly("io.reactivex.rxjava2:rxjava:2.2.21")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
