package com.wellcherish.texteditor.config

import com.wellcherish.texteditor.MainApplication
import java.io.File

/**
 * 编辑器的默认配置。后续的目标是该类中的所有变量都可以由用户配置。
 * */
object DefaultConfig {
    /**
     * 自动保存的时间间隔，默认是5秒。
     * */
    const val AUTO_SAVE_DURATION = 5000L
    /**
     * 保存文本文件的根目录。
     * */
    val saveRootDir: File = MainApplication.context.filesDir
    /**
     * 字符编码格式
     * */
    val charset = Charsets.UTF_8
    /**
     * 通常情况下，单页加载 10KB 到 50KB（约 5,000 到 25,000 个汉字） 是一个比较合理的范围。此处采用25000个字符。
     * */
    const val DEFAULT_TEXT_MAX_COUNT = 25000
}