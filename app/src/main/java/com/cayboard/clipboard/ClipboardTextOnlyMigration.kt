package com.cayboard.clipboard

import android.content.Context
import java.io.File

/** Compatibility cleanup only: no group/image runtime survives this upgrade. */
internal object ClipboardTextOnlyMigration {
    suspend fun migrate(context: Context) {
        val prefs = context.getSharedPreferences("clipboard_board", Context.MODE_PRIVATE)
        if (prefs.getBoolean("text_only_v1", false)) return
        context.getSharedPreferences("clipboard_images", Context.MODE_PRIVATE).edit().clear().commit()
        val cached = File(context.cacheDir, "clipboard_images").canonicalFile
        check(cached.parentFile == context.cacheDir.canonicalFile)
        if (cached.exists()) check(cached.deleteRecursively())
        val editor = prefs.edit()
        listOf("groups", "group_members", "hidden_categories", "tab_order", "group_record_order",
            "card_max_lines", "image_height", "pins").forEach(editor::remove)
        check(editor.putBoolean("text_only_v1", true).commit())
    }
}
