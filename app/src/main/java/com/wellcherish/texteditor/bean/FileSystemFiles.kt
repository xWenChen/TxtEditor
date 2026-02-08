package com.wellcherish.texteditor.bean

import java.io.File

class FileSystemFiles(
    /**
     * 未被删除的图片
     * */
    val contentFilesMap: Map<String, File>? = null,
    /**
     * 被删除的图片
     * */
    val deletedFilesMap: Map<String, File>? = null,
) {
    fun contains(filePath: String?): Boolean {
        filePath ?: return false
        return contentFilesMap?.contains(filePath) == true ||
                deletedFilesMap?.contains(filePath) == true
    }
}