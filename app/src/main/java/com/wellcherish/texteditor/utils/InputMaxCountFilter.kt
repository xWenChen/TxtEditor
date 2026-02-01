package com.wellcherish.texteditor.utils

import android.text.InputFilter
import android.text.Spanned

/**
 * 文本输入的过滤器，用于限制最大输入数量。
 * */
class InputMaxCountFilter(
    private val maxCount: Int,
    private val onTextCountToLimit: () -> Unit
    ) : InputFilter {
    /**
     * 返回值的含义：
     * - 返回 null：表示“没意见，按用户输入的来”。
     * - 返回 "" (空字符串)：表示“拒绝，什么都不准输入”。
     * - 返回 source.subSequence(...)：表示“只准一部分进去”（比如你做字数限制时的截断处理）。
     * */
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        if (source == null || dest == null) {
            return null
        }
        // 计算如果不拦截，输入后的总长度
        // 最大允许容量 - ( 现有长度 - 即将消失的长度 ) = 还能再放多少个
        val keep = maxCount - (dest.length - (dend - dstart))

        if (keep <= 0) {
            // 这里触发提示，因为输入即将被完全拦截
            onTextCountToLimit()
            // 返回空字符串，表示不接受任何新增输入
            return ""
        }
        if (keep >= end - start) {
            // 还能再放多少个，大于新字符串的长度，则保持原始输入
            return null
        }
        // 部分容纳（例如粘贴了一大段文字，只有前一部分能进去）
        onTextCountToLimit()
        return source.subSequence(start, start + keep)
    }
}