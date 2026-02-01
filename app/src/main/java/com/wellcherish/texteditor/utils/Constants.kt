package com.wellcherish.texteditor.utils

/**
 * 内存开销：Android 的 String 对象在内存中以 UTF-16 编码存储，一个字符占 2 字节。50KB 的文本在内存中仅占约 100KB。即使加上 UI 控件的额外开销，也不会触发 OOM（内存溢出）。
 * 渲染瓶颈：EditText 和 TextView 在处理极长文本时，**布局计算（Layout measure）**会导致主线程卡顿。
 *  - 超过 100KB：在低端机上滚动或初始加载时会有明显的掉帧。
 *  - 超过 1MB：极其容易引起 ANR（应用无响应）。
 * 用户体验：一屏手机通常只能显示约 500-1000 个汉字。一页 10KB-50KB 相当于提供了 10-50 屏的内容，既保证了翻页频率不会太高，又保证了加载速度。
 *
 * 此处选择11000个字符作为一页的内容，大概32K左右。
 */
const val PAGE_SIZE = 11000

/**
 * 文件的打开模式
 * */
enum class FileOpenMode(val modeStr: String) {
    READ("r"),
    READ_WRITE("rw")
}

/**
 * 文本内容的保存状态
 */
enum class SaveState {
    NOT_SAVE,
    SAVING,
    SAVED
}