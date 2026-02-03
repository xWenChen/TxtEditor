package com.wellcherish.texteditor.config

/**
 * 配置管理器。会缓存当前生效的配置。后续的目标是该类中的所有变量都可以由用户配置。
 * */
object ConfigManager {
    /**
     * 自动保存的时间间隔。
     * */
    var autoSaveDuration = DefaultConfig.AUTO_SAVE_DURATION

    var rootPath: String? = null

    var charset = DefaultConfig.charset

    var texMaxCount = DefaultConfig.DEFAULT_TEXT_MAX_COUNT

    var spanCount = DefaultConfig.GRID_SPAN_COUNT
}