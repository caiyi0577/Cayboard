package com.cayboard.settings

import android.content.Context
import java.io.File

/** Built-in schema files only. Network schema retrieval was removed from the offline build. */
object SchemaConfigHelper {
    fun parseSchemaListFromDefault(context: Context): List<String> = runCatching {
        context.assets.open("cayboard_base/default.yaml").bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.startsWith("- schema:") }
                .map { it.removePrefix("- schema:").trim() }.filter { it.isNotEmpty() }.toList()
        }
    }.getOrDefault(emptyList())
    fun checkSchemaFilesExist(context: Context, schemaId: String): Pair<Boolean, Boolean> {
        val dir = File(context.filesDir, "cayboard_input")
        return File(dir, "$schemaId.schema.yaml").isFile to File(dir, "$schemaId.dict.yaml").isFile
    }
    fun needsDownload(context: Context, schemaId: String): Boolean = !checkSchemaFilesExist(context, schemaId).first
    suspend fun downloadSchema(context: Context, schemaId: String): Boolean = false
    suspend fun downloadMissingSchemas(context: Context): List<String> = emptyList()
}
