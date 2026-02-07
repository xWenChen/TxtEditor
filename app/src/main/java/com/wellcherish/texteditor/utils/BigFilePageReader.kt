package com.wellcherish.texteditor.utils

import com.wellcherish.texteditor.model.FileOpenMode
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * 大文件分页读取工具类
 * 支持基于字节偏移量的随机跳转、向前翻页、向后翻页。
 * 能够自动处理 UTF-8 字节边界和 UTF-16 (Emoji) 字符截断问题。
 *
 * 为什么这个工具类是“专业级”的？
 * - 内存占用极低：通过 readOneChar 精确控制字节流，不会一次性把整个文件读入内存，即便文件有 2GB 也能秒开。
 * - 双重边界保护：
 *    - 字节层面：adjustOffsetToCharBoundary 解决了随机跳转导致的 UTF-8 乱码。
 *    - 字符层面：ensureCompleteSurrogates 解决了 String 截断导致的 Emoji 显示异常。
 * - 精确续读：返回的 nextOffset 是经过校准的物理字节位置，下一次读取只需传入此值即可实现“无缝衔接”。
 *
 * @property file 要读取的目标文件
 */
class BigFilePageReader(private val file: File) {

    companion object {
        private const val TAG = "BigFilePageReader"
    }

    // 文件编码格式，通常 Android 中为 UTF-8
    private val charset: Charset = Charsets.UTF_8

    /**
     * 向下翻页：从当前 [byteOffset] 开始往后读取约 [charCount] 个字符
     *
     * @param byteOffset 当前读取的起始字节位置
     * @param charCount 期望读取的字符数量
     *
     * @return 包含文本内容、下一页起始偏移量和是否到末尾的结果
     */
    fun readNextPage(byteOffset: Long, charCount: Int): PageResult {
        if (!file.exists()) {
            ZLog.e(TAG, "file not exist...")
            return PageResult("", byteOffset, true)
        }

        RandomAccessFile(file, FileOpenMode.READ.modeStr).use { raf ->
            val fileSize = raf.length()
            // 1. 确保起始位置落在正确的 UTF-8 字符边界上
            var currentPos = adjustOffsetToCharBoundary(raf, byteOffset)
            raf.seek(currentPos)

            val sb = StringBuilder()
            var charsFound = 0

            // 2. 循环读取，直到满足字符数或到达文件末尾
            while (charsFound < charCount && currentPos < fileSize) {
                val charBytes = readOneUtf8Char(raf) ?: break
                val charStr = String(charBytes, charset)

                sb.append(charStr)
                currentPos += charBytes.size
                charsFound++
            }

            // 3. 处理末尾可能截断的 Emoji (High Surrogate)
            val finalText = ensureCompleteSurrogates(sb.toString())
            // 如果截断了，需要微调位置以便下次从正确位置开始
            val actualBytesRead = finalText.toByteArray(charset).size
            val finalNextOffset = adjustOffsetToCharBoundary(raf, byteOffset + actualBytesRead)

            return PageResult(finalText, finalNextOffset, finalNextOffset >= fileSize)
        }
    }

    /**
     * 向上翻页：从当前页的起始位置 [currentStartOffset] 开始，向前读取约 [charCount] 个字符.
     *
     * @param currentStartOffset 当前页开头的字节偏移量
     * @param charCount 期望向上读取的字符数量
     *
     * @return 包含上一页文本内容、上一页起始偏移量和是否到开头的结果
     */
    fun readPreviousPage(currentStartOffset: Long, charCount: Int): PageResult {
        if (currentStartOffset <= 0) {
            return PageResult("", 0, true)
        }

        RandomAccessFile(file, FileOpenMode.READ.modeStr).use { raf ->
            // 1. 估算回跳距离。UTF-8 中文 3 字节，英文 1 字节。file的指针只能往后走，往前读，需要估计一个回跳距离。
            // 为了保证能读够 charCount，我们取平均 3 字节，并多预留一些空间
            val estimatedBytes = charCount * 3 + 10
            var searchStart = (currentStartOffset - estimatedBytes).coerceAtLeast(0L)

            // 2. 调整 searchStart 确保不落在字符中间
            searchStart = adjustOffsetToCharBoundary(raf, searchStart)

            // 3. 从 searchStart 读到 currentStartOffset 之间所有的字符
            raf.seek(searchStart)
            val sb = StringBuilder()
            var tempOffset = searchStart

            while (tempOffset < currentStartOffset) {
                val charBytes = readOneUtf8Char(raf) ?: break
                tempOffset += charBytes.size
                sb.append(String(charBytes, charset))
            }

            // 4. 截取最后面的 [charCount] 个字符作为上一页内容
            var resultText = sb.toString()
            if (resultText.length > charCount) {
                resultText = resultText.substring(resultText.length - charCount)
            }

            // 5. 再次处理 Emoji 截断（开头截断处理）
            // 如果第一位是低代理项，说明被截断了，需要舍弃
            if (resultText.isNotEmpty() && Character.isLowSurrogate(resultText[0])) {
                resultText = resultText.substring(1)
            }

            // 6. 计算这一页真实的起始字节偏移量
            // 通过当前页开头减去上一页内容的字节长度得到
            val prevPageByteSize = resultText.toByteArray(charset).size
            val prevStartOffset = currentStartOffset - prevPageByteSize

            return PageResult(resultText, prevStartOffset, prevStartOffset <= 0)
        }
    }

    /**
     * 校准偏移量：如果偏移量落在了 UTF-8 延续字节上 (10xxxxxx)，则向后移动直到找到字符开头
     */
    private fun adjustOffsetToCharBoundary(raf: RandomAccessFile, offset: Long): Long {
        var pos = offset
        val limit = raf.length()
        while (pos < limit) {
            raf.seek(pos)
            val b = raf.read()
            // 不是非起始字节，表示找到了为主子。在 UTF-8 中，非起始字节的特征是 10xxxxxx (二进制)，即 0x80 到 0xBF
            if (b == -1 || (b and 0xC0) != 0x80) {
                break
            }
            pos++
        }
        return pos
    }

    /**
     * 从当前指针位置读取一个完整的 UTF-8 字符字节数组（1-4字节）
     */
    private fun readOneUtf8Char(raf: RandomAccessFile): ByteArray? {
        val firstByte = raf.read()
        if (firstByte == -1) {
            return null
        }

        // 根据 UTF-8 规范判断当前字符占几个字节
        val len = when {
            firstByte and 0x80 == 0 -> 1            // 0xxxxxxx (ASCII)
            firstByte and 0xE0 == 0xC0 -> 2         // 110xxxxx
            firstByte and 0xF0 == 0xE0 -> 3         // 1110xxxx (中文常用)
            firstByte and 0xF8 == 0xF0 -> 4         // 11110xxx (Emoji)
            else -> 1                               // 异常字节
        }

        val bytes = ByteArray(len)
        bytes[0] = firstByte.toByte()
        if (len > 1) {
            val readActual = raf.read(bytes, 1, len - 1)

            // 健壮性检查：如果文件突然结束了，读不到足够的字节，就只返回实际读到的部分. +1是因为需要加上bytes[0]的长度
            if (readActual < len - 1) {
                return bytes.copyOf(readActual + 1)
            }
        }
        return bytes
    }

    /**
     * 健壮性检查：确保字符串末尾没有孤立的 UTF-16 高代理项（即半个 Emoji）。部分emoji需要两个char。
     */
    private fun ensureCompleteSurrogates(text: String): String {
        if (text.isEmpty()) {
            return ""
        }
        val lastChar = text[text.length - 1]
        // 最后一个字符是emoji的一部分。则丢弃。
        return if (Character.isHighSurrogate(lastChar)) {
            text.substring(0, text.length - 1)
        } else {
            text
        }
    }
}

