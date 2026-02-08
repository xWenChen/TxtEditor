package com.wellcherish.texteditor.database.dao

import androidx.room.*
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.database.constants.tableName

/**
 * 注意：Room 不支持挂起函数。
 * */
@Dao
interface FileItemDao {

    @Query("SELECT * FROM $tableName")
    fun queryAll(): List<FileItem>?

    @Query("SELECT * FROM $tableName WHERE id IN (:ids)")
    fun loadAllByIds(ids: LongArray): List<FileItem>?

    /**
     * 查询所有数据，按照文件更新时间逆序排序。
     *
     * ORDER BY createTime DESC // 按时间降序排序（从新到旧）
     * */
    @Query("SELECT * FROM $tableName where isDeleted = :deleted ORDER BY updateTime DESC")
    fun queryAllByTimeSort(deleted: Boolean): List<FileItem>?

    /**
     * 分页加载
     *
     * ORDER BY createTime ASC // 按时间升序排序（从旧到新）
     * ORDER BY createTime DESC // 按时间降序排序（从新到旧）
     *
     * LIMIT 1 限制查询单条
     * OFFSET 偏移值
     * */
    @Query("SELECT * FROM $tableName ORDER BY updateTime DESC LIMIT :size OFFSET :offset")
    fun queryByPage(size: Long, offset: Long): List<FileItem>?

    /**
     * LIMIT 1 限制查询单条
     * */
    @Query("SELECT * FROM $tableName WHERE contentId = :contentId LIMIT 1")
    fun queryByContentId(contentId: String): FileItem?

    @Insert
    fun insertAll(vararg data: FileItem)
    @Insert
    fun insertAll(list: List<FileItem>)

    /**
     * @return 成功更新的行数。
     * */
    @Update
    fun updateAll(vararg data: FileItem): Int
    @Update
    fun updateAll(list: List<FileItem>)

    /**
     * @return 成功删除的行数。
     * */
    @Delete
    fun deleteAll(vararg data: FileItem): Int
    /**
     * @return 成功删除的行数。
     * */
    @Delete
    fun deleteAll(list: List<FileItem>): Int
}