package com.wellcherish.texteditor.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.wellcherish.texteditor.page.SettingActivity

object PageUtils {
    fun jumpSettingPage(context: Context?) {
        context ?: return
        val intent = Intent(context, SettingActivity::class.java)
        context.startActivity(intent)
    }
}