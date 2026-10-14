package com.meowarex.rlmobile.network.utils

/** An equal-version remote beta must not replace the payload shipped by this Manager. */
fun preferBundledPatches(bundled: SemVer, available: SemVer?): Boolean =
    available == null || available <= bundled
