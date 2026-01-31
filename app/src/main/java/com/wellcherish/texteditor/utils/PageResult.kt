package com.wellcherish.texteditor.utils

/**
 * 分页读取结果类
 *
 * @property content 读取到的文本字符串
 * @property offset 下一页(或当前页)的字节起始位置
 * @property isBoundary 是否已经到达文件开头或结尾
 */
data class PageResult(
    val content: String,
    val offset: Long,
    val isBoundary: Boolean
)