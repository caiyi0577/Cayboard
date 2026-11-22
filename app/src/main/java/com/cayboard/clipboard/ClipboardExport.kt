package com.cayboard.clipboard

import org.json.JSONArray
import org.json.JSONObject
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal enum class ClipboardExportFormat(val label: String, val extension: String, val mimeType: String) {
    TXT("TXT", "txt", "text/plain"),
    MD("Markdown", "md", "text/markdown"),
    JSON("JSON", "json", "application/json"),
}

internal object ClipboardExport {
    private fun time(millis: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    /** Fixed records first, then history; newest first within each. */
    fun order(items: List<ClipboardItem>): List<ClipboardItem> =
        items.sortedByDescending { it.timestamp }

    fun fileName(format: ClipboardExportFormat, now: Long): String =
        "Cayboard剪贴板-${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))}.${format.extension}"

    
    fun write(items: List<ClipboardItem>, format: ClipboardExportFormat, out: Writer, now: Long) {
        when (format) {
            ClipboardExportFormat.TXT -> items.forEachIndexed { i, item ->
                if (i > 0) out.write("\n\n----------\n\n")
                out.write(item.text)
            }
            ClipboardExportFormat.MD -> {
                out.write("# Cayboard 剪贴板（${time(now)} 导出，${items.size} 条）\n")
                for ((title, group) in listOf("历史记录" to items)) {
                    if (group.isEmpty()) continue
                    out.write("\n## $title\n")
                    for (item in group) {

                        out.write("\n### ${time(item.timestamp)}\n\n")
                        // A fence longer than any backtick run inside keeps the text verbatim.
                        val fence = "`".repeat(maxOf(3, longestRun(item.text, '`') + 1))
                        out.write("$fence\n${item.text}\n$fence\n")
                    }
                }
            }
            ClipboardExportFormat.JSON -> {
                val array = JSONArray()
                for (item in items) array.put(JSONObject().apply {
                    put("text", item.text)

                    put("time", time(item.timestamp))
                    put("timestamp", item.timestamp)
                })
                out.write(JSONObject().put("app", "Cayboard").put("exported", time(now)).put("count", items.size)
                    .put("items", array).toString(2))
            }
        }
        out.flush()
    }

    private fun longestRun(text: String, c: Char): Int {
        var best = 0; var run = 0
        for (ch in text) { run = if (ch == c) run + 1 else 0; if (run > best) best = run }
        return best
    }
}
