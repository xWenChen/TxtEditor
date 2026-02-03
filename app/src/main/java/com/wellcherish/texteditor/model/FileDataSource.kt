package com.wellcherish.texteditor.model

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.wellcherish.texteditor.bean.FileItem
import com.wellcherish.texteditor.utils.ZLog
import com.wellcherish.texteditor.utils.content
import com.wellcherish.texteditor.utils.getFileTitle
import com.wellcherish.texteditor.utils.getSaveDir
import java.io.File

object FileDataSource {

    fun loadFiles(): List<FileItem> {
        val dir = getSaveDir()
        if (dir == null || !dir.isDirectory) {
            ZLog.e(TAG, "loadFiles, dir not exist")
            return emptyList()
        }
        // 按修改时间降序排序，最新的在前面
        return dir.listFiles()
            ?.sortedByDescending {
                it?.lastModified() ?: 0L
            }?.mapNotNull {
                it.toFileItem()
            } ?: emptyList()
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

    private fun File?.toFileItem(): FileItem? {
        this ?: return null

        return FileItem(
            this.absolutePath,
            this.getFileTitle(),
            this.content()
        )
    }

    private const val TAG = "FileDataSource"
}