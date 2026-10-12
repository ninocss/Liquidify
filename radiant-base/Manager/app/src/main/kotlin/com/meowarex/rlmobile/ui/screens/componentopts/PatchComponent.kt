package com.meowarex.rlmobile.ui.screens.componentopts

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.meowarex.rlmobile.manager.PathManager
import com.meowarex.rlmobile.network.utils.SemVer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.time.Instant

/**
 * A custom component that was deployed to this device with
 * the `deployWithAdb` task and imported by Manager.
 */
@Immutable
@Parcelize
@Serializable
data class PatchComponent(
    /**
     * The type of this custom component.
     */
    val type: Type,
    /**
     * The build version of this custom component.
     */
    val version: SemVer,
    /**
     * The time at which this custom component was deployed to the device and imported by manager.
     */
    val timestamp: Instant,
) : Parcelable {
    companion object {
        fun fromFileName(name: String, type: Type): PatchComponent? {
            val suffix = if (type == Type.Patches) ".zip" else ".apk"
            if (!name.endsWith(suffix)) return null
            val parts = name.removeSuffix(suffix).split('_', limit = 2)
            if (parts.size != 2) return null
            val timestamp = parts[0].toLongOrNull() ?: return null
            val version = SemVer.parseOrNull(parts[1]) ?: return null
            return PatchComponent(type, version, Instant.fromEpochMilliseconds(timestamp))
        }
    }

    @Parcelize
    @Serializable
    enum class Type : Parcelable {
        @SerialName("tidal")
        TidalApk,

        @SerialName("patches")
        Patches,
    }

    /**
     * Returns the imported file where this custom component should be stored.
     * This is not guaranteed to exist.
     */
    fun getFile(paths: PathManager): File {
        val dir = when (type) {
            Type.TidalApk -> paths.customTidalApksDir
            Type.Patches -> paths.customPatchesDir
        }
        val ext = when (type) {
            Type.TidalApk -> "apk"
            Type.Patches -> "zip"
        }

        // ${timestamp}_${componentVersion}.${componentFile.extension}
        return dir.resolve("${timestamp.toEpochMilliseconds()}_$version.$ext")
    }
}
