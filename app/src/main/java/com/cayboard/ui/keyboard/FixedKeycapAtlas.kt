package com.cayboard.ui.keyboard

import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** One image survives key disposal and page switches; concurrent preload/render requests share the load. */
internal class FixedKeycapAtlasCache {
    @Volatile private var image: ImageBitmap? = null

    fun get(load: () -> ImageBitmap): ImageBitmap {
        image?.let { return it }
        return synchronized(this) { image ?: load().also { image = it } }
    }
}

internal object FixedKeycapAtlas {
    private val cache = FixedKeycapAtlasCache()

    fun get(context: Context): ImageBitmap = cache.get {
        Trace.beginSection("Cayboard.loadFixedKeycapAtlas")
        val started = SystemClock.elapsedRealtime()
        try {
            val bitmap = context.applicationContext.assets.open("fixed-keyboard/dark/anjian.png").use {
                requireNotNull(BitmapFactory.decodeStream(it)) { "Cannot decode fixed keyboard keycaps" }
            }
            bitmap.prepareToDraw()
            bitmap.asImageBitmap().also {
                Log.i("FixedKeycapAtlas", "Loaded shared atlas in ${SystemClock.elapsedRealtime() - started}ms on ${Thread.currentThread().name}")
            }
        } finally {
            Trace.endSection()
        }
    }
}
