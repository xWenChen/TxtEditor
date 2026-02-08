package com.wellcherish.texteditor.model

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.wellcherish.texteditor.bean.FileSystemFiles
import com.wellcherish.texteditor.database.FileItemDatabase
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.utils.*
import java.io.File

object FileDataSource {

    private val dao = FileItemDatabase.db.fileItemDao()

    /**
     * 更新数据库
     * */
    fun updateDbData(
        insertList: List<FileItem>?,
        deleteList: List<FileItem>?,
        updateList: List<FileItem>?,
    ) {
        if (insertList != null) {
            dao.insertAll(insertList)
        }
        if (deleteList != null) {
            dao.deleteAll(deleteList)
        }
        if (updateList != null) {
            dao.updateAll(updateList)
        }
    }

    /**
     * 从文件系统加载所有文件数据。
     * */
    fun loadFilesFromFileSystem(): FileSystemFiles {
        return try {
            FileSystemFiles(
                getSaveDir()?.listFiles()?.filterNotNull()?.associateBy { it.absolutePath },
                getDeletedFilesDir()?.listFiles()?.filterNotNull()?.associateBy { it.absolutePath }
            )
        } catch (e: Exception) {
            ZLog.e(TAG, e)
            FileSystemFiles()
        }
    }

    /**
     * 从数据库查询记录。
     * */
    fun loadAllFiles(): List<FileItem> {
        return try {
            // 查询出未被删除的数据。
            dao.queryAll() ?: emptyList()
        } catch (e: Exception) {
            ZLog.e(TAG, e)
            emptyList()
        }
    }

    /**
     * 从数据库查询未删除的数据。
     * */
    fun loadNotDeletedFiles(): List<FileItem> {
        try {
            // 查询出未被删除的数据。
            val fileItems = dao.queryAllByTimeSort(false)
            if (fileItems.isNullOrEmpty()) {
                return emptyList()
            }
            fileItems.forEach {
                it.text = it.filePath.fileContent()
            }
            return fileItems
        } catch (e: Exception) {
            ZLog.e(TAG, e)
            return emptyList()
        }
    }

    /**
     * 更新或者插入数据
     * */
    fun updateOrInsertDbData(fileItem: FileItem): Boolean {
        runCatching {
            val oldItem = dao.queryByContentId(fileItem.contentId)
            if (oldItem == null) {
                // 新增
                dao.insertAll(fileItem)
                return true
            } else {
                // 更新
                return dao.updateAll(fileItem) > 0
            }
        }.onFailure {
            ZLog.e(TAG, it)
            return false
        }
        return false
    }

    /**
     * 通过MediaStore直接查询媒体数据库，进行分页加载
     *
     * 注：该方法未经验证，谨慎使用。
     *
     * @param page      页码，从 0 开始
     * @param pageSize  每页加载的数量
     */
    fun getMediaFilesPage(context: Context, page: Int, pageSize: Int): List<Uri> {
        val uriList = mutableListOf<Uri>()

        // 指定查询外部存储的文件集合
        val collection = MediaStore.Files.getContentUri("external")

        // 只需要获取 ID 来构建 Uri
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)

        // 计算偏移量 Offset
        val offset = page * pageSize

        // 构建查询参数 Bundle (API 26+)
        val queryArgs = Bundle().apply {
            // 1. 分页限制数量
            putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
            // 2. 分页偏移量
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            // 3. 排序规则：按添加时间倒序（最新的在前）
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )
        }

        try {
            context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    // 通过 ID 构建完整的 Content Uri
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    uriList.add(contentUri)
                }
            }
        } catch (e: Exception) {
            ZLog.e(TAG, e)
        }

        return uriList
    }

    private const val TAG = "FileDataSource"
}