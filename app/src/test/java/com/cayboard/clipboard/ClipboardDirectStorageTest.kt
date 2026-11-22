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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[34],application=android.app.Application::class)
class ClipboardDirectStorageTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun upgradeRemovesPinAndEditorColumnsButKeepsEveryRecord()=runBlocking<Unit> {
        val path=context.getDatabasePath("clipboard-no-pin-test.db")
        context.deleteDatabase(path.name);path.parentFile!!.mkdirs()
        AndroidSQLiteDriver().open(path.path).use {db->
            db.prepare("CREATE TABLE clipboard_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, text TEXT NOT NULL, code TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL DEFAULT 0, isPinned INTEGER NOT NULL DEFAULT 0, isQuickSend INTEGER NOT NULL DEFAULT 0, consumed INTEGER NOT NULL DEFAULT 0, searchKey TEXT NOT NULL DEFAULT '', fromDevice INTEGER NOT NULL DEFAULT 0, sensitive INTEGER NOT NULL DEFAULT 0)").use {it.step()}
            db.prepare("CREATE INDEX index_clipboard_entries_text ON clipboard_entries (text)").use {it.step()}
            db.prepare("INSERT INTO clipboard_entries VALUES (1,'普通记录','',100,0,0,0,'ptjl',0,0),(2,'原固定内容','code',200,1,0,1,'ygdnr',1,0),(3,'隐藏记录','',300,1,1,0,'',0,1)").use {it.step()}
            db.prepare("PRAGMA user_version=9").use {it.step()}
        }
        val db=Room.databaseBuilder<ClipboardDatabase>(context,path.name).setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO).addMigrations(ClipboardDatabase.MIGRATION_9_10).build()
        try {
            val dao=db.clipboardDao()
            val rows=dao.observeAll().first().sortedBy {it.id}
            assertEquals(listOf(1L,2L,3L),rows.map {it.id})
            assertEquals(listOf("普通记录","原固定内容","隐藏记录"),rows.map {it.text})
            assertEquals(listOf(100L,200L,300L),rows.map {it.timestamp})
            assertTrue(rows[1].consumed && rows[1].fromDevice)
            assertTrue(rows[2].sensitive)
            assertEquals(3,dao.countHistory())
            assertEquals(listOf(3L,2L,1L),dao.observeWindow(10).first().map {it.id})
            dao.clearAllClipboard()
            assertTrue(dao.observeAll().first().isEmpty())
        } finally {db.close()}
        AndroidSQLiteDriver().open(path.path).use {db->
            val columns=mutableListOf<String>()
            db.prepare("PRAGMA table_info(clipboard_entries)").use {while(it.step()) columns+=it.getText(1)}
            assertEquals(setOf("id","text","timestamp","consumed","searchKey","fromDevice","sensitive"),columns.toSet())
        }
        context.deleteDatabase(path.name)
    }
    @Test fun allRecordsUseHistoryRetentionAndSearchIsChronological()=runBlocking<Unit> {
        val db=Room.inMemoryDatabaseBuilder<ClipboardDatabase>(context).setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
        try {
            val dao=db.clipboardDao()
            (1L..5L).forEach {dao.insert(ClipboardEntry(text="记录$it",timestamp=it,searchKey="jl$it"))}
            assertEquals(listOf("记录5","记录4","记录3","记录2","记录1"),dao.search("%记录%",10).map {it.text})
            assertEquals(3,dao.trimHistory(2,Long.MIN_VALUE))
            assertEquals(listOf("记录5","记录4"),dao.observeAll().first().map {it.text})
        } finally {db.close()}
    }
    @Test fun legacyBackupPinAndCodeFieldsCannotRestoreTheFeature() {
        val records=ClipboardImport.parse("""{"items":[{"text":"旧文字","pinned":true,"code":"old","timestamp":123}]}""")
        assertEquals(listOf(ImportedClip("旧文字",123)),records)
    }
}
