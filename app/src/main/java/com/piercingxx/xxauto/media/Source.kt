package com.piercingxx.xxauto.media

/**
 * The two suite players xx-auto knows by name (AU6). Pure — no Android
 * imports — so the drive-screen models and their tests can key on it; the
 * matching `ComponentName`s live in [SuitePlayers].
 */
enum class Source(val packageName: String) {
    RADIO("com.skpp.radio"),
    AUDIOBOOK("com.piercingxx.audiobook"),
}
