package com.cayboard.settings

import android.text.InputType
import android.view.inputmethod.EditorInfo

object SensitiveInputState {
    @Volatile var sensitiveEditor: Boolean = false
    /**
     * 剪贴板只看这个：当前开着的是藏字的密码框，离开输入框就清掉。以前剪贴板看 [sensitiveEditor]，
     * 浏览器无痕页（NO_PERSONALIZED_LEARNING）、借「可见密码」关联想的地址栏和搜索框、离开密码框后
     * 没清的旧状态，都把网页上复制的普通文字当成密码藏起来（用户 11-09）。
     */
    @Volatile var passwordField: Boolean = false
    /**
     * Clipboard search is open: what is typed into it is never sent to plugins. Kept
     * apart from [sensitiveEditor], which every new input field rewrites.
     */
    @Volatile var clipboardSearch: Boolean = false
    val active: Boolean get() = sensitiveEditor || clipboardSearch

    fun isSensitiveEditor(attribute: EditorInfo?): Boolean {
        if (attribute == null) return false
        val cls = attribute.inputType and InputType.TYPE_MASK_CLASS
        val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
        val isPassword = when (cls) {
            InputType.TYPE_CLASS_TEXT -> variation in intArrayOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
        val noPersonalized = attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        return isPassword || noPersonalized
    }

    /** 藏字的密码框：文字密码、网页密码、数字密码。「可见密码」常被用来关联想，不算。 */
    fun isPasswordField(attribute: EditorInfo?): Boolean {
        if (attribute == null) return false
        val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
        return when (attribute.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}
