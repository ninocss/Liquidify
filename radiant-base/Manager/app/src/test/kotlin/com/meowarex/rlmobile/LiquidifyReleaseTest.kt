package com.meowarex.rlmobile

import com.meowarex.rlmobile.network.utils.SemVer
import com.meowarex.rlmobile.ui.screens.patchopts.*
import com.meowarex.rlmobile.ui.screens.componentopts.PatchComponent
import org.junit.Assert.*
import org.junit.Test

class LiquidifyReleaseTest {
    private val core = PatchSpec("LiquidifyCore", title = "Liquidify", source = "liquidify", defaultEnabled = true)

    @Test fun betaImportFileNamesRoundTripWithoutBeingDropped() {
        val imported = PatchComponent.fromFileName("1791798000000_1.0.0-beta.zip", PatchComponent.Type.Patches)
        assertNotNull(imported)
        assertEquals("1.0.0-beta", imported!!.version.toString())
        assertEquals(1791798000000L, imported.timestamp.toEpochMilliseconds())
        assertNull(PatchComponent.fromFileName("invalid_1.0.0-beta.zip", PatchComponent.Type.Patches))
        assertNull(PatchComponent.fromFileName("1791798000000_bad.zip", PatchComponent.Type.Patches))
        assertNull(PatchComponent.fromFileName("1791798000000_1.0.0-beta.apk", PatchComponent.Type.Patches))
    }

    @Test fun onlyOwnedAndExplicitlyCompatiblePatchesAreVisible() {
        val unowned = PatchSpec("Unclassified", title = "Unknown", defaultEnabled = true)
        val radiant = PatchSpec("LyricsRlApi", title = "Radiant Lyrics", source = "radiantLyrics", defaultEnabled = true)
        val allowed = radiant.copy(compatibleWith = listOf(core.id), fileNames = listOf("original.patch"))
        assertEquals(listOf(core), filterCompatiblePatchSpecs(listOf(core, unowned, radiant)))
        val result = filterCompatiblePatchSpecs(listOf(core, allowed))
        assertEquals(listOf(core, allowed.copy(defaultEnabled = false)), result)
        assertEquals("Radiant Lyrics", result.last().title)
        assertEquals(listOf("original.patch"), result.last().fileNames)
        assertEquals("radiantLyrics", result.last().source)
    }

    @Test fun missingAndTransitivelyHiddenDependenciesAreRejected() {
        val blocked = PatchSpec("Addon", title = "Addon", source = "radiantLyrics", compatibleWith = listOf(core.id), requires = listOf("Missing"))
        val dependent = core.copy(id = "Dependent", requires = listOf(blocked.id))
        assertEquals(listOf(core), filterCompatiblePatchSpecs(listOf(core, blocked, dependent)))
    }

    @Test fun hiddenRadiantFilesCannotEnterTheVisiblePatchPlan() {
        val hidden = PatchSpec("LyricsRlApi", title = "Radiant", fileNames = listOf("radiant.patch"))
        val visible = filterCompatiblePatchSpecs(listOf(core.copy(fileNames = listOf("liquidify.patch")), hidden))
        val options = PatchOptions.Default
        assertEquals(setOf("liquidify.patch"), options.knownPatchFiles(visible))
        assertFalse("radiant.patch" in options.knownPatchFiles(visible))
    }

    @Test fun betaVersionsRoundTripAndSortBeforeStable() {
        val beta = SemVer.parse("v1.0.0-beta")
        assertEquals("1.0.0-beta", beta.toString())
        assertTrue(beta < SemVer.parse("1.0.0"))
        assertTrue(SemVer.parse("1.0.0-beta.2") < SemVer.parse("1.0.0-beta.10"))
        assertTrue(beta > SemVer.parse("0.1.9-alpha1"))
        assertNull(SemVer.parseOrNull("1.0.0-"))
    }
}
