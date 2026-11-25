package com.cayboard.clipboard.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "clipboard_entries",
    indices = [Index(value = ["text"])]
)
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    @ColumnInfo(defaultValue = "0") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val consumed: Boolean = false,
    /** Pinyin initials and lowercase letters of the first characters, for search; "" = not computed yet. */
    @ColumnInfo(defaultValue = "") val searchKey: String = "",
    /** Received from a paired computer, phone or tablet rather than copied on this device. */
    @ColumnInfo(defaultValue = "0") val fromDevice: Boolean = false,
    /**
     * A password or card number kept because the user turned on 「记录密码等敏感内容」 (#95): shown as
     * dots, pasted privately, never searched or exported, deleted an hour after the copy.
     */
    @ColumnInfo(defaultValue = "0") val sensitive: Boolean = false,
    /** User-pinned records stay at the top and are excluded from automatic history trimming. */
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,

)
