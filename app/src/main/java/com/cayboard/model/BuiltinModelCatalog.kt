package com.cayboard.model

/** Versioned local catalog: downloads go directly to model publishers, without a market request. */
object BuiltinModelCatalog {
    /** Cayboard 自己 Release 里的原样转存（许可见该 Release 的 NOTICE-models.txt），海外线路优先。 */
    /** 日语词库（azooKey 词典 zip）和日语转换引擎（libCyAzooKey.so）。轻量版不带，装「日语」插件时下载（settings/JapanesePack.kt）。 */
    const val JAPANESE_DICTIONARY = "japanese-dictionary"
    const val JAPANESE_ENGINE = "japanese-engine"

    val models = com.cayboard.speech.SpeechModelCatalog.models + listOf(
        ModelInfo("ochwpro", "手写模型", "本地手写识别模型。", ModelCategory.HANDWRITING,
            // v1.1、v1.2：在 v1.0 上用用户贡献的手写样本微调（只改贡献题里常用字的输出层：v1.1 3 人 100 字，v1.2 45 人 180 字），
            // 字表不变。最新的放第一个。
            versions = listOf(ModelVersion(version = "v1.2", size = "6.7 MB",
                files = listOf(
                    ModelFile("ochwpro.onnx", "8a6095f9ccd222fa391d4e98e2795f478d3d88daaa62d4844677fac18c7acb02", 7001270L),
                    ModelFile("char_index.json", "171cf2ac23731428ac9dd28f64be82dece5f66a0ca3d674ffb3b567b51532176", 51317L),
                )),
                ModelVersion(version = "v1.1", size = "6.7 MB",
                files = listOf(
                    ModelFile("ochwpro.onnx", "d625e6b6fa0dc39499bc15a8ac960249fcea9e71e34334522d32524557d1136d", 7001270L),
                    ModelFile("char_index.json", "171cf2ac23731428ac9dd28f64be82dece5f66a0ca3d674ffb3b567b51532176", 51317L),
                )),
                ModelVersion(version = "v1.0", size = "6.7 MB",
                files = listOf(
                    ModelFile("ochwpro.onnx", "eb04d62a314c7d7bac4e34d6ce0c24137474d30ff94b929115a621385420ce13", 7001270L),
                    ModelFile("char_index.json", "171cf2ac23731428ac9dd28f64be82dece5f66a0ca3d674ffb3b567b51532176", 51317L),
                )))),
        ModelInfo("handwriting-formula", "公式手写模型", "手写数学公式，识别成 LaTeX；日语、英文手写的边写边学也需要它。", ModelCategory.HANDWRITING,
            versions = listOf(ModelVersion(version = "v1", size = "32.4 MB",
                files = listOf(
                    ModelFile("formula_encoder.onnx", "c2c55f14e8d3bc892c7f86ec9b237e54f46997300550a5c76501c54b7344b6dd", 21137800L),
                    ModelFile("formula_decoder.onnx", "ab11c1ceb4cb58b044be89b77c71eb32fcbc53ff1ddfc136c51444b002bdb2a9", 13346580L),
                    ModelFile("formula_vocab.json", "73653abb6993d5f2451501c804fb66671c05f86b4165bbe10cb3ef6ef96ec92c", 2003L),
                    ModelFile("formula_meta.json", "2ad149afaa9f3872f872e1d4bf9560dfb67be21d08d69ec1a0fa05441cff7541", 205L),
                )))),
        ModelInfo("candidate-gloss", "候选中英释义词典", "设置里“候选注释内容”选释义后，中文候选旁显示英文，中文键盘里的英文候选旁显示中文。英→中取自 ECDICT，中→英取自 CC-CEDICT（MDBG）；常见的日常短语也显示英文。", ModelCategory.OTHER,
            // v8：去掉俚语／粗口义项，多音字按 Rime 常用读音、义项按英文反查来选（v1 有 几号 → heroin）；补齐常用单字和词组；
            // 常见的说话用短语用电脑上离线翻译翻好、且译回中文一致的才收，不收带地名人名的（带 “+”，只显示）。
            // v10：单字只给常用且意思明确的（公开语料最常用约 870 字人工看过），生僻字不显示；公开聊天语料最常用的词组人工补齐、改正错义项；v11 常用实词不再被过严的筛子挡掉，补“X的”形容词；v12 不用截断的长释义，常用动词加“了”给过去式。
            versions = listOf(ModelVersion(version = "v12", size = "2.7 MB",
                files = listOf(
                    ModelFile("cyime_gloss-v12.tsv", "46ad175c5ea63cf1d9de5999aa615dbaf09a5689e71dc7d20c17e177fd22e761", 2747059L),
                ))))
    )
}
