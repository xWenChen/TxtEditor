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
) {
    /**
     * 文本内容
     * */
    @Ignore
    var text: CharSequence? = null

    constructor(
        id: Long,
        contentId: String,
        filePath: String?,
        title: String?,
        updateTime: Long,
        isDeleted: Boolean,
        text: CharSequence?
    ) : this(id, contentId, filePath, title, updateTime, isDeleted) {
        this.text = text
    }

    /**
     * 文件路径和删除态不影响页面展示。不比较。
     * */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileItem

        if (id != other.id) return false
        if (contentId != other.contentId) return false
        if (title != other.title) return false
        if (updateTime != other.updateTime) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + contentId.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + updateTime.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        return result
    }
}