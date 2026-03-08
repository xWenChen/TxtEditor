package com.wellcherish.texteditor.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wellcherish.base.R
import com.wellcherish.base.utils.StatusBarUtils
import com.wellcherish.texteditor.utils.colorRes

class ToolbarManager(
    activity: AppCompatActivity,
    private var toolbar: MainToolbar?,
    private val onSaveClick: ((View) -> Unit)? = null,
    private val onSettingClick: ((View) -> Unit)? = null,
) : DefaultLifecycleObserver {

    init {
        StatusBarUtils.setStatusBarCustom(activity, R.color.rb_green.colorRes, false)
        activity.lifecycle.addObserver(this)
        toolbar?.apply {
            setSaveClickListener(onSaveClick)
            setSettingClickListener(onSettingClick)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        toolbar = null
    }
}