package com.cayboard.ui.keyboard

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FixedKeycapAtlasCacheTest {
    @Test fun pageRecreationAndConcurrentPreloadDecodeOnlyOnce() {
        val cache = FixedKeycapAtlasCache()
        val image = ImageBitmap(2, 2)
        val loads = AtomicInteger()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val requests = (1..80).map {
                pool.submit<ImageBitmap> {
                    start.await()
                    cache.get { loads.incrementAndGet(); Thread.sleep(10); image }
                }
            }
            start.countDown()
            requests.forEach { assertSame(image, it.get(5, TimeUnit.SECONDS)) }
            // Leaving and remounting multiple pages must not evict the image.
            repeat(5) { repeat(40) { assertSame(image, cache.get { error("Decoded again on a page switch") }) } }
            assertEquals(1, loads.get())
        } finally { pool.shutdownNow() }
    }

    @Test fun failedPreloadCanRetryInsteadOfCachingFailure() {
        val cache = FixedKeycapAtlasCache()
        assertThrows(IllegalStateException::class.java) { cache.get { error("read failed") } }
        val image = ImageBitmap(2, 2)
        assertSame(image, cache.get { image })
    }
}
