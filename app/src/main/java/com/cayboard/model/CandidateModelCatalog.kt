package com.cayboard.model

/** Optional scoring resources; ordinary Chinese dictionaries do not depend on these downloads. */
object CandidateModelCatalog {
    const val GRAMMAR = "t9-grammar"
    const val SENTENCE = "t9-sentence"
    const val NGRAM = "t9-ngram"
    const val NGRAM_SMALL = "t9-ngram-small"
    val models = listOf(
        // 引擎按方案的 t9/ngram_model 在 rime 用户目录找 t9_ngram.lm；文件名必须保持 .lm。
        // 完整版与精简版写同一个文件名，装哪个九键就用哪个；紧凑格式（c3）内容与原 370 MB 文件相同，旧文件仍可读。
        ModelInfo(NGRAM, "九键词组模型(完整，203 MB)", "让九键按整句常用说法组词和排序，安装后重新启动输入法生效；不下载时九键使用旧算法。Cayboard 自行训练，默认推荐。", ModelCategory.CANDIDATE,
            versions = listOf(
            // 2026-11-05：语料里的「觉的」改成「觉得」（2445 处），其余与 v3-ls-tri-c3 相同；回放候选不变、「就」后 58333 出「觉得」。
            ModelVersion(version = "v3-ls-tri-jue1105-c3", size = "202.9 MB", changelog = "「觉的」改成「觉得」，其余不变。", files = listOf(
                ModelFile("t9_ngram.lm",
                    "8b2bc59bfeb961afb7f4e2338c64a1d19972f9be3bc8bf43afb3c164765b91bc", 212705538L),
            )),
            ModelVersion(version = "v3-ls-tri-c3", retired = true, changelog = "被 v3-ls-tri-jue1105-c3 取代（只修了「觉的」）。", size = "202.9 MB", files = listOf(
                ModelFile("t9_ngram.lm",
                    "11cee2cbd87f42cb18ddcd2b38ac2c2ef9e4a778c64da68f8c6f615e4050c867", 212706156L),
            )),
            // 旧版 370 MB 格式：只用来认出老版本留下的文件，让它在模型管理里可见、可删、可被新版替换；不作为下载版本。
            ModelVersion(version = "v3-ls-tri", retired = true, changelog = "旧格式，内容与新版相同；可删除后改装新版以节省空间。", size = "350.6 MB", files = listOf(
                ModelFile("t9_ngram.lm",
                    "766c21385a6d3e5ad22a0367373531d0c7bed4bc31074a5265da8ad1a3d46f12", 367598664L),
            )))),
        ModelInfo(NGRAM_SMALL, "九键词组模型(精简，105 MB)", "完整版剪枝后的小一半版本，整句首选约低 3 个百分点；和完整版二选一，安装后重新启动输入法生效。Cayboard 自行训练。", ModelCategory.CANDIDATE,
            versions = listOf(ModelVersion(version = "v4-ls-p5e-8-c3", size = "104.6 MB", files = listOf(
                ModelFile("t9_ngram.lm",
                    "7d346a1fa2b332bdd764d1b91bc8002526cb7d563fea091f6b861279be025c4e", 109685894L),
            )))),
    )

    internal val retiredWordModels = models.filter { it.id == NGRAM || it.id == NGRAM_SMALL }
    internal val offeredModels = models - retiredWordModels.toSet()
    internal const val RETIRED_WORD_MODEL_FILE = "t9_ngram.lm"
}
