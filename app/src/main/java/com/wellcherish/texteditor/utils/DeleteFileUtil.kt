package com.wellcherish.texteditor.utils

import com.wellcherish.texteditor.bean.FileData
import com.wellcherish.texteditor.model.FileRepository
import java.io.File

object DeleteFileUtil {
    private const val TAG = "DeleteFileUtil"
    /**
     * 文件引入应用回收站目录。数据库数据标记为删除态。
     * */
    suspend fun recycleFile(data: FileData?): Boolean {
        data ?: return false

        if (!moveFileToRecycle(data.dbData?.filePath)) {
            ZLog.e(TAG, "recycleFile, move file failed.")
            return false
        }

        // 变更数据库状态
        val dbData = data.dbData
        if (dbData == null) {
            ZLog.e(TAG, "recycleFile, dbData = null.")
            return false
        }
        dbData.isDeleted = true
        return FileRepository.updateOrInsertDbData(dbData)
    }

    /**
     * 文件放入到回收站目录
     * */
    private fun moveFileToRecycle(filePath: String?): Boolean {
        filePath ?: return false

        // 如果重命名失败（例如跨分区），则执行 复制+删除
        try {
            val srcFile = File(filePath)
            val dstFile = File(getDeletedFilesDir(), srcFile.name)
            if (!dstFile.exists()) {
                dstFile.createNewFile()
            }
            srcFile.reader().use { src ->
                dstFile.writer().use { dst ->
                    dst.write(src.readText())
                }
            }
            return srcFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}