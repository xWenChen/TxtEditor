package com.wellcherish.texteditor.model

import com.wellcherish.texteditor.bean.FileItem

object FileRepository {
    private val source = FileDataSource

    /**
     * 列举目录下的所有文件。
     * */
    fun loadFiles(): List<FileItem> {
        return source.loadFiles()
    }
}