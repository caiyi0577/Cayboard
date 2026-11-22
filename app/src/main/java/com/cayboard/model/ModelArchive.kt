package com.cayboard.model

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** 把下载/安装失败换成用户看得懂、知道下一步做什么的一句话。 */
internal fun modelFailureText(error: Throwable): String {
    val chain = generateSequence(error) { it.cause }.take(8).toList()
    val text = chain.mapNotNull { it.message }.joinToString(" ")
    return when {
        "ENOSPC" in text || "No space left" in text -> "存储空间不够，清理一些空间后点重试"
        // 这条的 cause 是旧文件的校验失败，要先于“不完整”判断。
        "保留已有不同版本" in text -> "输入法目录里已有一份同名的自定义文件，为免覆盖没有安装；移走它后点重试"
        "校验失败" in text || "不完整" in text -> "模型文件不完整，请从安装包重新准备"
        else -> "模型准备失败：${chain.firstNotNullOfOrNull { it.message } ?: "未知原因"}。可点重试"
    }
}

internal object ModelArchive {
    internal fun extractTarBz2(archiveFile: File, targetDir: File) {
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 65536).use { bis ->
                BZip2CompressorInputStream(bis).use { bzIn ->
                    TarArchiveInputStream(bzIn).use { tarIn ->
                        var entry = tarIn.nextEntry
                        while (entry != null) {
                            val rawName = entry.name
                            val parts = rawName.split("/", limit = 2)
                            val entryName = if (parts.size > 1) parts[1] else rawName

                            if (entryName.isNotEmpty() && !entry.isDirectory) {
                                if (!entry.isFile) throw IOException("不支持的模型归档条目")
                                val outputFile = modelDownloadFile(targetDir, entryName)
                                outputFile.parentFile?.mkdirs()
                                FileOutputStream(outputFile).use { out ->
                                    val buffer = ByteArray(8192)
                                    var len: Int
                                    while (tarIn.read(buffer).also { len = it } != -1) {
                                        out.write(buffer, 0, len)
                                    }
                                }
                            }
                            entry = tarIn.nextEntry
                        }
                    }
                }
            }
        }
    }
}
