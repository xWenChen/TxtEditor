package com.wellcherish.texteditor.bean

import android.view.View
import com.wellcherish.texteditor.model.SettingType

data class SettingItem(
    val type: SettingType,
    val text: String,
    val noDoubleClick: (View, Int, SettingItem) -> Unit,
)