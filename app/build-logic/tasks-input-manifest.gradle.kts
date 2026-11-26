import java.security.MessageDigest

val manifestOutput = layout.buildDirectory.dir("generated/input-manifest")
val roots = listOf(
    "cayboard_base" to file("src/main/assets/cayboard_base"),
    "cayboard_pinyin" to layout.buildDirectory.dir("generated/chinese-assets/cayboard_pinyin").get().asFile,
    "cayboard_chinese" to file("src/main/assets/cayboard_chinese"),
    "cayboard_chinese" to layout.buildDirectory.dir("generated/stroke-assets/cayboard_chinese").get().asFile,
    "cayboard_chinese" to layout.buildDirectory.dir("generated/cangjie-assets/cayboard_chinese").get().asFile,
)
val prepareInputManifest by tasks.registering {
    dependsOn("prepareChineseDictionaries", "prepareStrokeDictionary", "prepareCangjieDictionary")
    inputs.files(roots.map { it.second })
    outputs.dir(manifestOutput)
    doLast {
        val entries = sortedMapOf<String, String>()
        for ((assetRoot, root) in roots) root.walkTopDown().filter { it.isFile }.forEach { source ->
            val relative = source.relativeTo(root).invariantSeparatorsPath
            if (assetRoot == "cayboard_base" && relative !in setOf("pinyin_simp.dict.yaml", "pinyin_simp_ext.dict.yaml")) return@forEach
            // 长句重排已停用，两种包都不带这个模型（与 build.gradle.kts 的资源忽略规则一致）。
            if (assetRoot == "cayboard_chinese" && relative == "t9_sentence.onnx") return@forEach
            if (relative.endsWith(".md") || relative.startsWith("LICENSE") || relative.endsWith(".custom.yaml")) return@forEach
            // Optional layouts are installed only when missing; a user's edited schema stays theirs.
            if (assetRoot == "cayboard_pinyin" && relative in setOf("pinyin_qwjrtk.schema.yaml", "pinyin_cyletix10.schema.yaml", "pinyin_colemak.schema.yaml", "pinyin_typewise.schema.yaml")) return@forEach
            val target = relative
            val digest = MessageDigest.getInstance("SHA-256")
            source.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            entries[target] = "$hash\t${source.length()}\t$assetRoot/$relative\t$target"
        }
        val output = manifestOutput.get().asFile.apply { mkdirs() }
        File(output, "cayboard-bundled-manifest.tsv").writeText(entries.values.joinToString("\n", postfix = "\n"))
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(prepareInputManifest) }
