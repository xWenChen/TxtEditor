package com.wellcherish.texteditor.model

import com.wellcherish.texteditor.bean.FileSystemFiles
import com.wellcherish.texteditor.database.bean.FileItem

object FileRepository {
    private val source = FileDataSource

    /**
     * 更新数据库
     * */
    fun updateDbData(
        insertList: List<FileItem>?,
        deleteList: List<FileItem>?,
        updateList: List<FileItem>?,
    ) {
        source.updateDbData(insertList, deleteList, updateList)
    }

    /**
     * 从文件系统加载所有文件数据。
     * */
    fun loadFilesFromFileSystem(): FileSystemFiles {
        return source.loadFilesFromFileSystem()
    }

    fun loadAllFiles(): List<FileItem> {
        return source.loadAllFiles()
    }

    /**
     * 列举目录下的所有文件。
     * */
    fun loadNotDeletedFiles(): List<FileItem> {
        return source.loadNotDeletedFiles()
    }

    /**
     * 更新或者插入数据
     * */
    fun updateOrInsertDbData(fileItem: FileItem): Boolean {
        return source.updateOrInsertDbData(fileItem)
    }
}