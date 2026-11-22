package com.cayboard.clipboard

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cayboard.clipboard.db.ClipboardDatabase
import com.cayboard.clipboard.db.ClipboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 剪贴板去重（用户 10-30）：同一段文字只留一条，首尾空白不算区别；固定内容和它的编码不丢。 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class ClipboardDedupeTest {
    private val database = Room.inMemoryDatabaseBuilder<ClipboardDatabase>(
        ApplicationProvider.getApplicationContext<Context>(),
    ).setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    private val dao = database.clipboardDao()

    @After fun close() = database.close()

    private suspend fun all() = dao.observeAll().first()

    @Test fun copyingTheSameTextWithDifferentEndsKeepsOneRecordWithTheNewestText() = runBlocking {
        assertTrue(dao.upsert("送到前台", 100))
        assertFalse(dao.upsert("送到前台\n", 200))
        assertFalse(dao.upsert("　送到前台 ", 300))
        val records = all()
        assertEquals(1, records.size)
        assertEquals("　送到前台 ", records.single().text)
        assertEquals(300L, records.single().timestamp)
        // Different text inside still counts as different.
        assertTrue(dao.upsert("送到 前台", 400))
        assertEquals(2, all().size)
    }


}
