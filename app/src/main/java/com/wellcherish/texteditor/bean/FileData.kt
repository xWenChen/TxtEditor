package com.wellcherish.texteditor.bean

import com.wellcherish.texteditor.database.bean.FileItem

data class FileData(
    var dbData: FileItem? = null,
    var text: CharSequence? = null,
    var showDelete: Boolean = false
)