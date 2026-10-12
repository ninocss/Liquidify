plugins {
    id("com.android.library") version "9.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}
val dexTools = configurations.create("dexTools")
dexTools.attributes {
    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attribute(org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
        objects.named(org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM))
}
dependencies {
    dexTools("com.android.tools.smali:smali:3.0.9")
    dexTools("com.android.tools.smali:smali-baksmali:3.0.9")
    dexTools("com.aliucord:axml:1.0.1")
    dexTools("com.aliucord:binary-resources:2.1.0")
    dexTools("org.slf4j:slf4j-nop:2.0.17")
    dexTools("io.github.java-diff-utils:java-diff-utils:4.16")
}
tasks.register<Copy>("prepareDexTools") {
    from(dexTools)
    into(layout.buildDirectory.dir("dex-tools"))
    doLast {
        dexTools.files.filter { it.extension == "aar" }.forEach { aar ->
            copy {
                from(zipTree(aar)) { include("classes.jar"); rename { aar.nameWithoutExtension + ".jar" } }
                into(layout.buildDirectory.dir("dex-tools"))
            }
        }
    }
}
