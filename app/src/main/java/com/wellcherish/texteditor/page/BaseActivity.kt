package com.wellcherish.texteditor.page

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wellcherish.texteditor.ui.MainToolbar
import com.wellcherish.texteditor.ui.ToolbarManager
import com.wellcherish.texteditor.utils.PageUtils

open class BaseActivity : AppCompatActivity() {
    protected var toolbarManager: ToolbarManager? = null

    override fun onDestroy() {
        super.onDestroy()
        toolbarManager = null
    }

    protected fun initToolbar(
        toolbar: MainToolbar?,
        onSaveClick: ((View) -> Unit)? = null,
        onSettingClick: ((View) -> Unit)? = { PageUtils.jumpSettingPage(it.context) },
    ) {
        toolbar ?: return
        toolbarManager = ToolbarManager(
            this,
            toolbar,
            onSaveClick = onSaveClick,
            onSettingClick = onSettingClick
        )
    }
}