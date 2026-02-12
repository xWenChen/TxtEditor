package com.wellcherish.texteditor.database.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.wellcherish.texteditor.database.constants.tableName

/**
 * 文件数据记录。
 *
 * Room 不允许默认值。Room 不允许多构造函数。
 * */
@Entity(tableName = tableName)
class FileItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    /**
     * 内容id，可以唯一确定一条数据
     * */
    @ColumnInfo
    val contentId: String,
    /**
     * 文件地址
     * */
    @ColumnInfo
    var filePath: String?,
    /**
     * 文件标题
     * */
    @ColumnInfo
    var title: String?,
    /**
     * 文件更新时间，精确到毫秒
     * */
    @ColumnInfo
    var updateTime: Long,
    /**
     * 文件是否被移入回收站的标识。
     * */
    @ColumnInfo
    var isDeleted: Boolean,
)