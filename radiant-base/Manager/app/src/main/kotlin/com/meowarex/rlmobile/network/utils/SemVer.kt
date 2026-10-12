package com.meowarex.rlmobile.network.utils

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Parses a Semantic version in the format of `v1.0.0` or `1.0.0`.
 * This always gets serialized and stringified without the `v` prefix.
 */
@Immutable
@Parcelize
@Serializable(SemVer.Serializer::class)
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null,
) : Comparable<SemVer>, Parcelable {
    override fun compareTo(other: SemVer): Int {
        var cmp = 0
        if (0 != major.compareTo(other.major).also { cmp = it })
            return cmp
        if (0 != minor.compareTo(other.minor).also { cmp = it })
            return cmp
        if (0 != patch.compareTo(other.patch).also { cmp = it })
            return cmp

        if (prerelease == other.prerelease) return 0
        if (prerelease == null) return 1
        if (other.prerelease == null) return -1
        val left = prerelease.split('.')
        val right = other.prerelease.split('.')
        for (i in 0 until minOf(left.size, right.size)) {
            val a = left[i]; val b = right[i]
            val an = a.toLongOrNull(); val bn = b.toLongOrNull()
            val order = when {
                an != null && bn != null -> an.compareTo(bn)
                an != null -> -1
                bn != null -> 1
                else -> a.compareTo(b)
            }
            if (order != 0) return order
        }
        return left.size.compareTo(right.size)
    }

    override fun equals(other: Any?): Boolean {
        val ver = other as? SemVer
            ?: return false

        return ver.major == major &&
            ver.minor == minor &&
            ver.patch == patch && ver.prerelease == prerelease
    }

    override fun toString(): String {
        return "$major.$minor.$patch" + (prerelease?.let { "-$it" } ?: "")
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (prerelease?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun parse(version: String): SemVer = parseOrNull(version)
            ?: throw IllegalArgumentException("Invalid semver string $version")

        fun parseOrNull(version: String): SemVer? {
            val normalized = version.removePrefix("v").substringBefore('+')
            val prerelease = normalized.substringAfter('-', "").takeIf { it.isNotEmpty() }
            if ('-' in normalized && prerelease == null) return null
            if (prerelease != null && !Regex("[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*").matches(prerelease)) return null
            val parts = normalized.substringBefore('-').split(".")

            if (parts.size != 3)
                return null

            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null

            if (major < 0 || minor < 0 || patch < 0) return null
            return SemVer(major, minor, patch, prerelease)
        }
    }

    object Serializer : KSerializer<SemVer> {
        override val descriptor = PrimitiveSerialDescriptor("SemVer", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) =
            parse(decoder.decodeString())

        override fun serialize(encoder: Encoder, value: SemVer) {
            encoder.encodeString(value.toString())
        }
    }
}
