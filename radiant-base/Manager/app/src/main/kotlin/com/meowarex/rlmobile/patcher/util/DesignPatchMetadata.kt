package com.meowarex.rlmobile.patcher.util

import android.os.Build
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest

@Serializable
data class DesignPatchMetadata(
    val compatibility: DesignCompatibility? = null,
    val designResources: DesignResources? = null,
)

@Serializable
data class DesignCompatibility(
    val apkSha256: String,
    val minSdk: Int,
    val capabilities: List<String> = emptyList(),
    val strictSquarePatches: Boolean = true,
) {
    fun validate(apk: File) {
        require(Build.VERSION.SDK_INT >= minSdk) { "Liquidify requires Android API $minSdk or newer." }
        require(capabilities.all { it in setOf("exactBaseSha256", "squareResourcesV1", "squareRuntimeThemeV1") }) {
            "This design pack needs a newer Liquidify Manager."
        }
        require(apkSha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid design-pack APK fingerprint." }
        require(sha256(apk) == apkSha256) {
            "This design pack requires the original TIDAL 2.200.0 (10004) APK. Select the matching APK; no changes were applied."
        }
    }
}

@Serializable
data class DesignResources(
    val patchId: String,
    val palette: List<DesignColor> = emptyList(),
    val fontsPatchId: String,
    val fonts: List<DesignFont> = emptyList(),
    val themeXml: List<DesignXml> = emptyList(),
)

@Serializable
data class DesignColor(val id: String, val name: String, val expectedType: String,
    val expectedData: String, val light: String, val dark: String)

@Serializable
data class DesignFont(val target: String, val source: String, val originalSha256: String, val sha256: String)

@Serializable
data class DesignXml(val target: String, val attributeId: String, val expectedType: Int,
    val fromResource: String, val toResource: String, val expectedCount: Int,
    val originalSha256: String, val sha256: String)

fun sha256(file: File): String = file.inputStream().use { stream ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(128 * 1024)
    while (true) {
        val count = stream.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }
