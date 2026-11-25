package com.cayboard.clipboard.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.cayboard.clipboard.ClipboardCapacity
import kotlinx.coroutines.flow.Flow

/**
 * Characters ignored at both ends when two clips are compared: copying "地址" and "地址\n" (or with a
 * full-width / no-break space) is the same record (user 10-30: 剪贴板去重).
 */
internal const val CLIP_TRIM = "' ' || char(9) || char(10) || char(13) || char(12288) || char(160)"

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_entries ORDER BY isPinned DESC, timestamp DESC")
    fun observeAll(): Flow<List<ClipboardEntry>>

    
    @Query("SELECT * FROM clipboard_entries ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    fun observeWindow(limit: Int): Flow<List<ClipboardEntry>>

    
    @Query("SELECT COUNT(*) FROM clipboard_entries ")
    fun observeHistoryCount(): Flow<Int>

    
    @Query("SELECT * FROM clipboard_entries WHERE sensitive = 0 AND " +
        "(text LIKE :pattern ESCAPE '\\' OR searchKey LIKE :pattern ESCAPE '\\') " +
        "ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    suspend fun search(pattern: String, limit: Int): List<ClipboardEntry>

    
    @Query("SELECT * FROM clipboard_entries WHERE sensitive = 0 AND searchKey LIKE :pattern " +
        "ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    suspend fun searchKey(pattern: String, limit: Int): List<ClipboardEntry>

    /** Initials as 9-key digits ([pattern] holds digits): the search key read through the keypad. */
    @Query("SELECT * FROM clipboard_entries WHERE sensitive = 0 AND " +
        com.cayboard.clipboard.ClipboardSearch.T9_KEY_SQL +
        " LIKE :pattern ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    suspend fun searchKeyDigits(pattern: String, limit: Int): List<ClipboardEntry>

    @Query("SELECT id, substr(text, 1, 2000) AS text FROM clipboard_entries WHERE searchKey = '' AND sensitive = 0 LIMIT :limit")
    suspend fun missingSearchKeys(limit: Int): List<SearchKeySource>

    @Query("UPDATE clipboard_entries SET searchKey = :key WHERE id = :id")
    suspend fun setSearchKey(id: Long, key: String)

    

    @Query("SELECT * FROM clipboard_entries WHERE text = :text ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun findByText(text: String): ClipboardEntry?

    
    @Query("SELECT * FROM clipboard_entries WHERE trim(text, " + CLIP_TRIM + ") = trim(:text, " + CLIP_TRIM + ") " +
        "ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun findSameText(text: String): ClipboardEntry?


    
    @Query("DELETE FROM clipboard_entries WHERE id != :keepId AND " +
        "trim(text, " + CLIP_TRIM + ") = trim(:text, " + CLIP_TRIM + ")")
    suspend fun deleteHistoryCopies(text: String, keepId: Long)

    
    @Query("DELETE FROM clipboard_entries WHERE id NOT IN (SELECT id FROM (SELECT id, MAX(timestamp) FROM clipboard_entries GROUP BY trim(text, " + CLIP_TRIM + ")))")
    suspend fun deleteDuplicateHistory(): Int

    /** A copy differing only in surrounding whitespace replaces the history record's text. */
    @Query("UPDATE clipboard_entries SET text = :text, searchKey = :searchKey, timestamp = :timestamp, consumed = 0 " +
        "WHERE id = :id")
    suspend fun replaceCopiedText(id: Long, text: String, searchKey: String, timestamp: Long)


    /** Atomic and idempotent; no deduplication may discard different codes on equal text. */

    @Query("SELECT * FROM clipboard_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ClipboardEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ClipboardEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ClipboardEntry>)

    @Query("UPDATE clipboard_entries SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long)


    // 再次复制是新的用户动作，同一条历史记录也应重新出现在预览栏。
    @Query("UPDATE clipboard_entries SET timestamp = :timestamp, consumed = 0 WHERE id = :id")
    suspend fun refreshCopiedItem(id: Long, timestamp: Long)


    @Query("DELETE FROM clipboard_entries WHERE id = :id")
    suspend fun deleteClipboardById(id: Long)

    @Query("DELETE FROM clipboard_entries WHERE id IN (:ids)")
    suspend fun deleteClipboardByIds(ids: List<Long>)

    @Query("DELETE FROM clipboard_entries ")
    suspend fun clearAllClipboard()

    @Query("DELETE FROM clipboard_entries ")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM clipboard_entries ")
    suspend fun countHistory(): Int

    @Query("DELETE FROM clipboard_entries WHERE id IN (SELECT id FROM clipboard_entries WHERE isPinned = 0 ORDER BY timestamp ASC LIMIT :limit)")
    suspend fun trimOldest(limit: Int)

    @Query("SELECT COUNT(*) FROM clipboard_entries WHERE isPinned = 0 AND timestamp < :cutoff")
    suspend fun countHistoryBefore(cutoff: Long): Int

    @Query("DELETE FROM clipboard_entries WHERE isPinned = 0 AND timestamp < :cutoff")
    suspend fun deleteHistoryBefore(cutoff: Long): Int

    
    @Transaction
    suspend fun trimHistory(maxCount: Int, cutoff: Long): Int {
        var removed = deleteHistoryBefore(cutoff)
        if (maxCount > 0) {
            val over = countUnpinned() - maxCount
            if (over > 0) { trimOldest(over); removed += over }
        }
        return removed
    }

    // An edit clears the search key; the backfill recomputes it from the new text.

    @Query("UPDATE clipboard_entries SET consumed = 1 WHERE id = :id")
    suspend fun markConsumed(id: Long)

    @Query("DELETE FROM clipboard_entries")
    suspend fun deleteAll()

    @Transaction
    suspend fun upsertAndTrim(text: String, now: Long, maxItems: Int, searchKey: String = "") {
        if (upsert(text, now, searchKey)) {
            val unpinned = countUnpinned()
            if (unpinned > maxItems) {
                trimOldest(unpinned - maxItems)
            }
        }
    }

    
    @Transaction
    suspend fun upsert(text: String, now: Long, searchKey: String = "", fromDevice: Boolean = false,
        sensitive: Boolean = false): Boolean {
        val existing = findSameText(text)
        val keep = when {
            existing == null -> insert(ClipboardEntry(text = text, timestamp = now, searchKey = searchKey,
                fromDevice = fromDevice, sensitive = sensitive))
            existing.text != text -> existing.id.also { replaceCopiedText(it, text, searchKey, now) }
            else -> existing.id.also { refreshCopiedItem(it, now) }
        }
        // Copied here again: a received record becomes this device's own. A received copy of text
        // already here only moves it up.
        if (existing != null && existing.fromDevice && !fromDevice) setFromDevice(keep, false)
        // The latest copy decides: copied as a password, the record is hidden (and expires); copied
        // again from an ordinary place, an unfixed one is ordinary again. A fixed one stays hidden.
        if (existing != null && existing.sensitive != sensitive) {
            setSensitive(keep, sensitive)
            if (sensitive) setSearchKey(keep, "")
        }
        deleteHistoryCopies(text, keep)
        return existing == null
    }

    @Query("UPDATE clipboard_entries SET fromDevice = :fromDevice WHERE id = :id")
    suspend fun setFromDevice(id: Long, fromDevice: Boolean)

    @Query("UPDATE clipboard_entries SET sensitive = :sensitive WHERE id = :id")
    suspend fun setSensitive(id: Long, sensitive: Boolean)

    @Query("UPDATE clipboard_entries SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM clipboard_entries WHERE isPinned = 0")
    suspend fun countUnpinned(): Int

    
    @Query("DELETE FROM clipboard_entries WHERE sensitive = 1 AND isPinned = 0 AND timestamp < :cutoff")
    suspend fun deleteSensitiveBefore(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM clipboard_entries WHERE fromDevice = 1")
    suspend fun countReceived(): Int

    @Query("SELECT COALESCE(SUM(length(CAST(text AS BLOB)) + length(searchKey)), 0) FROM clipboard_entries " +
        "WHERE fromDevice = 1")
    suspend fun receivedBytes(): Long

    @Query("DELETE FROM clipboard_entries WHERE id IN (SELECT id FROM clipboard_entries " +
        "WHERE fromDevice = 1 ORDER BY timestamp ASC, id ASC LIMIT :limit)")
    suspend fun deleteOldestReceived(limit: Int)

    
    @Transaction
    suspend fun addReceived(text: String, now: Long, searchKey: String, maxCount: Int, maxBytes: Long): Boolean {
        val added = upsert(text, now, searchKey, fromDevice = true)
        countReceived().let { if (it > maxCount) deleteOldestReceived(it - maxCount) }
        while (receivedBytes() > maxBytes && countReceived() > 0) deleteOldestReceived(1)
        return added
    }

    
    @Transaction


    
    @Query("SELECT COALESCE(SUM(length(CAST(text AS BLOB)) + length(searchKey)), 0) FROM clipboard_entries " +
        "WHERE fromDevice = 0")
    fun observeStoredBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(length(CAST(text AS BLOB)) + length(searchKey)), 0) FROM clipboard_entries " +
        "WHERE fromDevice = 0")
    suspend fun storedBytes(): Long

    
    @Query("SELECT id, length(CAST(text AS BLOB)) + length(searchKey) AS bytes FROM clipboard_entries " +
        "WHERE fromDevice = 0 AND isPinned = 0 AND id != :keep " +
        "ORDER BY timestamp ASC, id ASC")
    suspend fun ownHistorySizes(keep: Long): List<ClipboardRecordSize>

    
    @Transaction
    suspend fun makeRoom(adding: Long, capacity: Long, keep: Long = -1): Boolean {
        val stored = storedBytes()
        if (ClipboardCapacity.accepts(stored, adding, capacity)) return true
        val history = ownHistorySizes(keep)
        val remove = ClipboardCapacity.oldestToRemove(history.map { it.bytes }, stored, adding, capacity) ?: return false
        history.take(remove).map { it.id }.chunked(500).forEach { deleteClipboardByIds(it) }
        return true
    }


    
    @Transaction
    suspend fun importRecord(text: String, timestamp: Long, searchKey: String, canAdd: Boolean): ClipboardImportRow {
        findSameText(text)?.let { return ClipboardImportRow(it.id, ClipboardImportRow.EXISTING) }
        if (!canAdd) return ClipboardImportRow(0, ClipboardImportRow.NO_ROOM)
        val id = insert(ClipboardEntry(text=text,timestamp=timestamp,searchKey=searchKey))
        return ClipboardImportRow(id,ClipboardImportRow.ADDED)
    }

}

/** Where one imported record went: [id] of its row, and whether it was [ADDED], already there, or left out. */
data class ClipboardImportRow(val id: Long, val outcome: Int) {
    companion object {
        const val ADDED = 0
        const val EXISTING = 1
        const val NO_ROOM = 2
    }
}

/** The part of a record the search-key backfill needs. */
data class SearchKeySource(val id: Long, val text: String)

/** A history record's stored size, for making room. */
data class ClipboardRecordSize(val id: Long, val bytes: Long)
