package com.meowarex.rlmobile.manager

import android.util.Log
import com.meowarex.rlmobile.BuildConfig
import com.meowarex.rlmobile.network.models.RLBuildInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Snapshot of the last successful release fetch (build info + patches asset URL)
@Serializable
data class CachedReleaseInfo(
    val data: RLBuildInfo,
    val patchesAssetUrl: String,
)

object ReleaseInfoCache {
    fun load(paths: PathManager, json: Json): CachedReleaseInfo? = try {
        paths.cachedReleaseInfo
            .takeIf { it.exists() }
            ?.let { json.decodeFromString<CachedReleaseInfo>(it.readText()) }
    } catch (t: Throwable) {
        Log.w(BuildConfig.TAG, "Failed to read cached release info", t)
        null
    }

    fun save(paths: PathManager, json: Json, info: CachedReleaseInfo) = try {
        paths.cachedReleaseInfo.parentFile?.mkdirs()
        paths.cachedReleaseInfo.writeText(json.encodeToString(info))
    } catch (t: Throwable) {
        Log.w(BuildConfig.TAG, "Failed to cache release info", t)
    }
}
