package com.wellcherish.texteditor.utils

import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.config.DefaultConfig
import java.io.File
import java.io.FileReader
import java.util.*

private const val TAG = "FileEditors"

const val FILE_NAME_PART = "TextEditor"

const val FILE_NAME_SPLIT = "_"

/**
 * 文件存储的目录
 * */
const val SAVE_DIR = "TextEditor/content"
/**
 * todo 被删除的文件，存放在这个目录下。这个目录充当了回收站的作用。
 * */
const val DELETED_FILE_DIR = "TextEditor/deletedFile"

/**
 * 文件默认的存储路径是APP内部目录
 * */


/**
 * 使用字符流读取文件内容。
 *
 * java的流处理主要分为字节流和字符流。字节流有截断的风险。
 *
 * - 字节流：InputStream/OutputStream
 * - 字符流：Reader/Writer
 *
 * @return 读取到的文本内容，如果失败，则返回null。
 * */
fun String?.fileContent(): String? {
    if (this == null) {
        ZLog.e(TAG, "fileContent, file path=null")
        return null
    }
    return File(this).content()
}

/**
 * 使用字符流读取文件内容。
 *
 * java的流处理主要分为字节流和字符流。字节流有截断的风险。
 *
 * - 字节流：InputStream/OutputStream
 * - 字符流：Reader/Writer
 *
 * @return 读取到的文本内容，如果失败，则返回null。
 * */
fun File?.content(): String? {
    if (this == null || !this.exists()) {
        ZLog.e(TAG, "file not exist, path=${this?.absolutePath}")
        return null
    }
    return FileReader(this).use { reader -> reader.readText() }
}

fun File?.getFileTitle(): String? {
    this ?: return null
    return this.name.split(FILE_NAME_SPLIT).firstOrNull()
}

/**
 * - 文件名的生成规则为：标题_TextEditor_Unix时间戳_TextEditor.txt。
 * - 最终的标题会取第一个部分的字符串。
 *
 * @param fileSuffix 文件名后缀
 *
 * @return 最终的文件名。
 * */
fun getFileName(title: String, fileSuffix: String = ".txt"): String {
    return title + FILE_NAME_SPLIT +
            FILE_NAME_PART + FILE_NAME_SPLIT +
            System.currentTimeMillis() + FILE_NAME_SPLIT +
            FILE_NAME_PART +
            fileSuffix
}

/**
 * 获取当前缓存的根目录。
 * */
fun getSaveDir(): File? {
    return try {
        val rootDir = ConfigManager.rootPath.let {
            if (it.isNullOrBlank()) {
                DefaultConfig.saveRootDir
            } else {
                File(it)
            }
        }

        val currentSaveDir = File(rootDir, SAVE_DIR)

        if (!currentSaveDir.exists()) {
            // 创建目录
            currentSaveDir.mkdirs()
        }

        currentSaveDir
    } catch (e: Exception) {
        ZLog.e(TAG, e)
        null
    }
}

/**
 * 获取被删除文件的根目录。
 * */
fun getDeletedFilesDir(): File? {
    return try {
        val rootDir = ConfigManager.rootPath.let {
            if (it.isNullOrBlank()) {
                DefaultConfig.saveRootDir
            } else {
                File(it)
            }
        }

        val dir = File(rootDir, DELETED_FILE_DIR)

        if (!dir.exists()) {
            // 创建目录
            dir.mkdirs()
        }

        dir
    } catch (e: Exception) {
        ZLog.e(TAG, e)
        null
    }
}

fun CharSequence?.safeTitle(): CharSequence {
    if (this.isNullOrBlank()) {
        return R.string.default_title_name.stringRes
    }
    return this
}

/**
 * 随机内容id，uuid
 * */
fun generateContentId(): String {
    return UUID.randomUUID().toString().replace("-", "")
}