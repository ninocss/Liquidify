package com.meowarex.rlmobile.patcher

import com.meowarex.rlmobile.patcher.steps.download.*
import com.meowarex.rlmobile.patcher.steps.install.*
import com.meowarex.rlmobile.patcher.steps.patch.*
import com.meowarex.rlmobile.patcher.steps.prepare.*
import com.meowarex.rlmobile.ui.screens.patchopts.PatchOptions
import kotlinx.collections.immutable.persistentListOf

class TidalPatchRunner(
    options: PatchOptions,
) : StepRunner() {
    override val steps = persistentListOf(
        // Prepare
        FetchInfoStep(localPatches = options.customPatches != null),
        DowngradeCheckStep(options),
        RestoreDownloadsStep(),

        // Download
        DownloadTidalStep(options.customTidalApk),
        DownloadPatchesStep(options.customPatches),
        CopyDependenciesStep(),

        // Patch
        SmaliPatchStep(options),
        DesignResourcesStep(options),
        ReorganizeDexStep(),
        PatchManifestStep(options),
        PatchCertsStep(),
        SaveMetadataStep(options),

        // Install
        AlignmentStep(),
        SigningStep(options),
        InstallStep(options),
        CleanupStep(),
    )
}
