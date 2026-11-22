package com.cayboard.service

import com.cayboard.rime.RimeEngine
import com.cayboard.rime.RimeProcessResult
import com.cayboard.settings.JapaneseSchemas

/**
 * jaroomaji 的 Return 绑定提交格式化后的假名读音，不能用原始罗马音替代。
 * 返回值已取走原生 commit；调用方只提交 committedText 一次，并保留引擎返回的剩余组合。
 */
internal fun RimeEngine.processJapaneseEnterIfComposing(): RimeProcessResult? {
    val schemaId = getCurrentSchema()
    if (!com.cayboard.settings.InputProfiles.describe(schemaId).capabilities.japaneseConversion) return null
    if (getInput().isEmpty()) return null
    return processKeyAndGetResult(0xff0d, 0)
}
