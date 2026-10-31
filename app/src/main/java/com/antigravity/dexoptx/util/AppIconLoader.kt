package com.antigravity.dexoptx.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconLoader {
    // 150 items in memory cache is ~6MB total, keeping scrolling ultra-fluid with zero memory pressure
    private val memoryCache = object : LruCache<String, ImageBitmap>(150) {}

    fun getCached(packageName: String): ImageBitmap? {
        return synchronized(memoryCache) {
            memoryCache.get(packageName)
        }
    }

    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        synchronized(memoryCache) {
            memoryCache.get(packageName)?.let { return@withContext it }
        }

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable: Drawable = appInfo.loadIcon(pm)

            val density = context.resources.displayMetrics.density
            val targetPx = (48 * density).toInt().coerceIn(48, 144)

            val bitmap: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                val orig = drawable.bitmap
                if (orig.width in 1..targetPx && orig.height in 1..targetPx) {
                    orig
                } else if (orig.width > 0 && orig.height > 0) {
                    Bitmap.createScaledBitmap(orig, targetPx, targetPx, true)
                } else {
                    val bmp = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, targetPx, targetPx)
                    drawable.draw(canvas)
                    bmp
                }
            } else {
                val bmp = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, targetPx, targetPx)
                drawable.draw(canvas)
                bmp
            }

            val imageBitmap = bitmap.asImageBitmap()
            synchronized(memoryCache) {
                memoryCache.put(packageName, imageBitmap)
            }
            imageBitmap
        } catch (_: Throwable) {
            null
        }
    }

    fun clear() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }
}
