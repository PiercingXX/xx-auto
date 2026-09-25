package com.piercingxx.xxauto.log

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * The suite log-dump door at `${applicationId}.logs`. Phase 6 wires it to the
 * `AppLog` ring buffer: the query returns one row per collected log file
 * (name, text), same shape as xx-camera.
 */
class LogDumpProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("name", "text"))
        // The ring buffer + last crash, combined — the "share" view. Exposed
        // here so the suite store can collect the live ring even before the
        // rotating files flush, not just the on-disk files below.
        val share = AppLog.shareText()
        if (share.isNotBlank()) {
            cursor.addRow(arrayOf("combined.txt", share.takeLast(FILE_CAP)))
        }
        for (file in AppLog.dumpFiles()) {
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            cursor.addRow(arrayOf(file.name, text.takeLast(FILE_CAP)))
        }
        return cursor
    }
    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.piercingxx.logs"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val FILE_CAP = 200_000
    }
}