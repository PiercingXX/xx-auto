package com.piercingxx.xxauto.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap

/**
 * Loads a session's custom-button icon (design Phase 3): the `iconResId` out
 * of the owning package via `createPackageContext`, or the `iconUri`. Null
 * means "draw the display name instead". Cached, because the card recomposes
 * every second while playing.
 */
object ButtonIcons {

    private val cache = LruCache<String, ImageBitmap>(32)
    private val misses = mutableSetOf<String>()

    fun load(context: Context, packageName: String, iconResId: Int, iconUri: String?, sizePx: Int): ImageBitmap? {
        val key = "$packageName/$iconResId/$iconUri/$sizePx"
        cache.get(key)?.let { return it }
        if (key in misses) return null
        val bitmap = runCatching {
            when {
                iconResId != 0 -> {
                    val res = context.createPackageContext(packageName, 0).resources
                    ResourcesCompat.getDrawable(res, iconResId, null)
                        ?.toBitmap(sizePx, sizePx)
                        ?.asImageBitmap()
                }
                iconUri != null -> context.contentResolver.openInputStream(Uri.parse(iconUri))
                    ?.use { BitmapFactory.decodeStream(it) }
                    ?.let { Bitmap.createScaledBitmap(it, sizePx, sizePx, true) }
                    ?.asImageBitmap()
                else -> null
            }
        }.getOrNull()
        if (bitmap == null) misses += key else cache.put(key, bitmap)
        return bitmap
    }
}
