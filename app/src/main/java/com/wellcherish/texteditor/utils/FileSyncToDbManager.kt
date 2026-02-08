package com.wellcherish.texteditor.utils

import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.model.FileRepository
import java.io.File

/**
 * 文件系统数据同步到数据库的管理器，每次启动应用时，会做一次检查。
 * */
object FileSyncToDbManager {
    private const val TAG = "FileSyncToDbManager"
    /**
     * 同步数据
     * */
    fun trySync() {
        runCatching {
            sync()
        }.onFailure {
            ZLog.e(TAG, it)
        }
    }

    private fun sync() {
        val repo = FileRepository
        // 读取所有数据库数据
        val dbItemsMap = repo.loadAllFiles().associateBy { it.filePath }
        // 读取所有未删除和删除了的内容
        val files = repo.loadFilesFromFileSystem()

        val insertList = mutableListOf<FileItem>()
        val updateList = mutableListOf<FileItem>()
        val deletedList = mutableListOf<FileItem>()
        // 对比数据库记录和文件系统的文件
        // 数据在文件系统，不在数据库，需要插入数据库
        checkInsertOrUpdateFiles(dbItemsMap, files.contentFilesMap, insertList, updateList, false)
        checkInsertOrUpdateFiles(dbItemsMap, files.deletedFilesMap, insertList, updateList, true)
        // 数据在数据库，不在文件系统，需要从数据库删除
        dbItemsMap.forEach { (filePath, fileItem) ->
            // 数据库数据不在文件系统
            if (!files.contains(filePath)) {
                deletedList.add(fileItem)
            }
        }

        // 更新数据库
        repo.updateDbData(insertList, deletedList, updateList)
    }

    private fun checkInsertOrUpdateFiles(
        dbItemsMap: Map<String?, FileItem>,
        fileMap: Map<String, File>?,
        insertList: MutableList<FileItem>,
        updateList: MutableList<FileItem>,
        isDeleted: Boolean
    ) {
        fileMap?.forEach { (filePath, file) ->
            val existingItem = dbItemsMap[filePath]
            if (existingItem != null) {
                // 如果数据库里有，更新状态并加入更新列表
                existingItem.updateTime = file.lastModified()
                if (isDeleted) {
                    existingItem.isDeleted = true
                }
                updateList.add(existingItem)
                return@forEach
            }
            // 如果数据库里没有，创建新对象并加入插入列表
            insertList.add(
                FileItem(
                    0L,
                    generateContentId(),
                    filePath,
                    file.getFileTitle(),
                    file.lastModified(),
                    isDeleted
                )
            )
        }
    }
}