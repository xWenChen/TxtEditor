package com.wellcherish.texteditor.config

import com.wellcherish.texteditor.MainApplication

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
    val saveRootDir = MainApplication.context.filesDir
}