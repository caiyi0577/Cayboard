package com.cayboard.ui.menubar

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.clipboard.ClipboardItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The "close the clipboard after a paste" switch: on by default, configured in the main settings. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w800dp-h700dp-hdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClipboardCloseAfterPasteTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val prefs get() = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("clipboard_board", Context.MODE_PRIVATE)

    @Test fun onByDefaultSoTheClipboardClosesAsBefore() {
        prefs.edit().clear().commit()
        assertTrue(clipboardClosesAfterPaste(prefs))
        prefs.edit().putBoolean(CLIPBOARD_CLOSE_AFTER_PASTE_KEY, false).commit()
        assertFalse(clipboardClosesAfterPaste(ApplicationProvider.getApplicationContext<Context>()))
    }

}
