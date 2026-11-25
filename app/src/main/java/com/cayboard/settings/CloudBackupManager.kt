package com.cayboard.settings

import android.content.Context
import android.util.Base64
import com.cayboard.BuildConfig
import java.io.File
import java.security.KeyStore
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Full application-data backup stored directly on a WebDAV service such as Nutstore. */
object CloudBackupManager {
    const val DEFAULT_URL = "https://dav.jianguoyun.com/dav/"
    private const val MAX_BACKUP_BYTES = 4L * 1024 * 1024 * 1024
    private const val BACKUP_FILE = "Cayboard-full-backup.zip"
    private const val MANIFEST_FILE = "Cayboard-full-backup.manifest"
    private const val PART_BYTES = 400L * 1024 * 1024
    private const val KEY_ALIAS = "cayboard-cloud-backup"

    fun verifyCredentials(baseUrl: String, username: String, password: String): Result<Unit> = runCatching {
        require(baseUrl.startsWith("https://", ignoreCase = true)) { "坚果云地址必须使用 HTTPS" }
        require(username.isNotBlank() && password.isNotBlank()) { "请输入坚果云注册邮箱和第三方应用密码" }
        // OPTIONS is a standard WebDAV capability request supported by Android's
        // HttpURLConnection and avoids unsupported PROPFIND/MKCOL methods.
        val first = webDavStatus(baseUrl, username, password, "OPTIONS")
        check(first in 200..399 || first == 405) {
            if (first == 404) "坚果云目录不存在，请先在坚果云网页端创建英文目录" else "坚果云目录验证失败：HTTP $first"
        }
    }

    fun backup(context: Context, baseUrl: String, username: String, password: String): Result<Long> = runCatching {
        require(username.isNotBlank() && password.isNotBlank()) { "请输入坚果云账号和应用密码" }
        val temp = File(context.cacheDir, "cayboard-backup-${System.currentTimeMillis()}.zip")
        try {
            writeArchive(context, temp)
            uploadParts(baseUrl, username, password, temp)
            temp.length()
        } finally { temp.delete() }
    }

    fun restore(context: Context, baseUrl: String, username: String, password: String): Result<Long> = runCatching {
        require(username.isNotBlank() && password.isNotBlank()) { "请输入坚果云账号和应用密码" }
        val temp = File(context.cacheDir, "cayboard-restore-${System.currentTimeMillis()}.zip")
        try {
            downloadParts(baseUrl, username, password, temp)
            restoreArchive(context, temp)
            temp.length()
        } finally { temp.delete() }
    }

    private fun writeArchive(context: Context, target: File) {
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            val manifest = "Cayboard\nformat=full-1\nversion=${BuildConfig.VERSION_NAME}\ncreatedAt=${System.currentTimeMillis()}\n"
            zip.putNextEntry(ZipEntry("_cayboard_backup/manifest.txt")); zip.write(manifest.toByteArray()); zip.closeEntry()
            val dataRoot = context.dataDir
            dataRoot.listFiles().orEmpty().filterNot { it.name in setOf("cache", "code_cache", "shared_prefs") }.forEach { root ->
                addTree(zip, root, dataRoot, "data")
            }
            val prefs = File(dataRoot, "shared_prefs")
            prefs.listFiles().orEmpty().filterNot { it.name == "cloud_backup.xml" }.forEach { addTree(zip, it, dataRoot, "data") }
        }
    }

    private fun addTree(zip: ZipOutputStream, file: File, root: File, prefix: String) {
        if (file.isDirectory) {
            file.listFiles().orEmpty().forEach { addTree(zip, it, root, prefix) }
            return
        }
        val relative = file.relativeTo(root).path.replace('\\', '/')
        if (relative.isBlank() || relative.startsWith("../")) return
        zip.putNextEntry(ZipEntry("$prefix/$relative"))
            file.inputStream().buffered().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun restoreArchive(context: Context, archive: File) {
        val stage = File(context.cacheDir, "cayboard-restore-stage-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var manifest = false
            ZipInputStream(archive.inputStream().buffered()).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    require(!entry.name.contains("..") && !entry.name.startsWith("/")) { "备份包路径无效" }
                    if (entry.name == "_cayboard_backup/manifest.txt") { manifest = true; input.closeEntry(); continue }
                    require(entry.name.startsWith("data/")) { "备份包格式不受支持" }
                    val target = File(stage, entry.name.removePrefix("data/")).canonicalFile
                    require(target.path.startsWith(stage.canonicalPath + File.separator)) { "备份包路径越界" }
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        target.outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                    input.closeEntry()
                }
            }
            check(manifest) { "不是 Cayboard 全量备份" }
            val dataRoot = context.dataDir
            stage.listFiles().orEmpty().filterNot { it.name in setOf("cache", "code_cache") }.forEach { source ->
                val target = File(dataRoot, source.name)
                target.deleteRecursively()
                source.copyRecursively(target, overwrite = true)
            }
        } finally { stage.deleteRecursively() }
    }

    /** The field is a WebDAV directory URL; Nutstore's official endpoint is /dav/. */
    private fun endpoint(base: String): String = base.trimEnd('/') + "/" + BACKUP_FILE

    private fun uploadParts(base: String, user: String, password: String, file: File) {
        val count = ((file.length() + PART_BYTES - 1) / PART_BYTES).coerceAtLeast(1).toInt()
        File(file.parentFile, "${file.name}.manifest").writeText("$count\n${file.length()}\n")
        File(file.parentFile, "${file.name}.manifest").also { webDavPut(base, user, password, MANIFEST_FILE, it) }.delete()
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            for (index in 1..count) {
                val part = File(file.parentFile, "${file.name}.$index")
                part.outputStream().use { output ->
                    var left = PART_BYTES
                    while (left > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                        if (read < 0) break
                        output.write(buffer, 0, read); left -= read
                    }
                }
                webDavPut(base, user, password, partName(index, count), part)
                part.delete()
            }
        }
    }

    private fun downloadParts(base: String, user: String, password: String, target: File) {
        val manifest = File(target.parentFile, "${target.name}.manifest")
        webDavGet(base, user, password, MANIFEST_FILE, manifest)
        val lines = manifest.readLines()
        val count = lines.firstOrNull()?.toIntOrNull() ?: error("坚果云备份清单损坏")
        val expected = lines.getOrNull(1)?.toLongOrNull() ?: error("坚果云备份清单损坏")
        target.outputStream().use { output ->
            for (index in 1..count) {
                val part = File(target.parentFile, "${target.name}.$index")
                webDavGet(base, user, password, partName(index, count), part)
                part.inputStream().use { it.copyTo(output) }
                part.delete()
            }
        }
        manifest.delete()
        check(target.length() == expected) { "坚果云备份分片不完整" }
    }

    private fun partName(index: Int, count: Int): String =
        "Cayboard-full-backup.zip.part-%03d-of-%03d".format(index, count)

    private fun webDavPut(base: String, user: String, password: String, name: String, file: File) {
        ensureDirectory(base, user, password)
        val connection = (URL(base.trimEnd('/') + "/" + name).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"; doOutput = true; setFixedLengthStreamingMode(file.length().coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            setRequestProperty("Authorization", auth(user, password)); setRequestProperty("Content-Type", "application/zip")
            connectTimeout = 20_000; readTimeout = 120_000
        }
        file.inputStream().use { input -> connection.outputStream.use { output -> input.copyTo(output) } }
        check(connection.responseCode in 200..299) { "坚果云上传失败：HTTP ${connection.responseCode} ${serverError(connection)}" }
        connection.disconnect()
    }

    private fun ensureDirectory(base: String, user: String, password: String) {
        val code = webDavStatus(base, user, password, "OPTIONS")
        check(code in 200..399 || code == 405) {
            if (code == 404) "坚果云目录不存在，请先在坚果云网页端创建英文目录" else "坚果云目录验证失败：HTTP $code"
        }
    }

    private fun webDavStatus(base: String, user: String, password: String, method: String): Int {
        val connection = (URL(base.trimEnd('/') + "/").openConnection() as HttpURLConnection).apply {
            requestMethod = method; doOutput = method == "MKCOL"; setRequestProperty("Authorization", auth(user, password))
            connectTimeout = 20_000; readTimeout = 30_000
        }
        val code = connection.responseCode
        connection.disconnect()
        return code
    }

    private fun webDavGet(base: String, user: String, password: String, name: String, target: File) {
        val connection = (URL(endpoint(base)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; setRequestProperty("Authorization", auth(user, password))
            connectTimeout = 20_000; readTimeout = 120_000
        }
        check(connection.responseCode in 200..299) { "坚果云下载失败：HTTP ${connection.responseCode} ${serverError(connection)}" }
        var total = 0L
        connection.inputStream.use { input -> target.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024); var count: Int
            while (input.read(buffer).also { count = it } >= 0) { total += count; require(total <= MAX_BACKUP_BYTES) { "备份文件过大" }; output.write(buffer, 0, count) }
        } }
        connection.disconnect()
    }

    private fun auth(user: String, password: String): String =
        "Basic " + Base64.encodeToString("$user:$password".toByteArray(), Base64.NO_WRAP)

    private fun serverError(connection: HttpURLConnection): String = runCatching {
        connection.errorStream?.bufferedReader()?.use { it.readText().take(240) }.orEmpty()
    }.getOrDefault("")

    fun encryptCredential(context: Context, value: String): String {
        if (value.isBlank()) return ""
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            generator.init(android.security.keystore.KeyGenParameterSpec.Builder(KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generator.generateKey()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_ALIAS, null)) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }

    fun decryptCredential(context: Context, encoded: String): String = runCatching {
        if (encoded.isBlank()) return ""
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, keyStore.getKey(KEY_ALIAS, null), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
    }.getOrDefault("")
}
