package com.piercingxx.xxauto.backup

import com.piercingxx.suite.backup.BackupContents
import com.piercingxx.suite.backup.SuiteBackupProvider
import java.io.File

/**
 * xx-auto's door in the suite backup contract
 * (xx-apps/docs/SUITE-BACKUP-PROVIDER.md). Prefs only — the app's settings
 * live in one DataStore file (AU11) and its launcher theme-sync state lives in
 * one shared_prefs file ([com.piercingxx.xxauto.ui.theme.ThemeStore]); the
 * snapshot is exactly those two files and nothing else. No databases, no loose
 * files, no exports.
 */
class AutoBackupProvider : SuiteBackupProvider() {

    override val appName: String get() = "xx-auto"

    override fun contents(): BackupContents {
        // AU11: the settings DataStore file lives under files/datastore/.
        // Enumerate it explicitly rather than leaning on the generic
        // defaultContents() so the backup contract is precise and verifiable —
        // if a stray shared_prefs ever appears it is not silently swept into the
        // archive.
        //
        // DataStore writes the committed file as `<name>.preferences_pb` and
        // stages an in-flight write as `<name>.preferences_pb.tmp`. The `.tmp`
        // is transient and may be half-written when a snapshot lands mid-edit;
        // capturing it would hand the suite store a corrupt entry that a restore
        // would then drop back into files/datastore/. Only the committed file is
        // backed up.
        val datastoreDir = File(snapshot.filesDir, "datastore")
        val prefs = datastoreDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") }
            ?.sortedBy { it.name }
            ?: emptyList()

        // ThemeStore (ui/theme/ThemeStore.kt) writes the launcher theme-sync
        // state through the older SharedPreferences API, so it lands in
        // shared_prefs/theme_sync.xml rather than the DataStore file. It is
        // part of "everything the app knows" and must survive a backup/restore
        // cycle; enumerate it explicitly alongside the DataStore file.
        val themeSync = File(snapshot.sharedPrefsDir, "theme_sync.xml")
        val allPrefs = (prefs + listOf(themeSync))
            .filter { it.isFile }
            .sortedBy { it.name }

        return BackupContents(
            prefs = allPrefs,
            databases = emptyList(),
            files = emptyList(),
            exports = emptyMap(),
        )
    }
}