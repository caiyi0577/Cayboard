package com.cayboard.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.settings.SensitiveInputState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Emoji, kaomoji and symbol recents follow the same no-learning rule as typing. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class RecentUsagePrivacyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val saved = Pair(SensitiveInputState.sensitiveEditor, SensitiveInputState.clipboardSearch)

    @After fun restore() {
        SensitiveInputState.sensitiveEditor = saved.first
        SensitiveInputState.clipboardSearch = saved.second
    }

    @Test fun recentsDoNotChangeInSearchOrPasswordFields() {
        SensitiveInputState.sensitiveEditor = false; SensitiveInputState.clipboardSearch = false
        val key = RecentUsageStore.KEY_RECENT_EMOJIS
        assertEquals(listOf("😀"), RecentUsageStore.record(context, key, "😀"))

        SensitiveInputState.clipboardSearch = true
        assertEquals(listOf("😀"), RecentUsageStore.record(context, key, "🙈"))
        SensitiveInputState.clipboardSearch = false

        SensitiveInputState.sensitiveEditor = true
        assertEquals(listOf("😀"), RecentUsageStore.record(context, key, "🔑"))
        SensitiveInputState.sensitiveEditor = false

        assertEquals(listOf("😀"), RecentUsageStore.get(context, key))
        assertEquals(listOf("👍", "😀"), RecentUsageStore.record(context, key, "👍"))
    }
}
