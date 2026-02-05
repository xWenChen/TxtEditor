package com.wellcherish.texteditor.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ToolbarManager(
    activity: AppCompatActivity,
    private var toolbar: MainToolbar?,
    private val onAddClick: ((View) -> Unit)? = null,
    private val onSaveClick: ((View) -> Unit)? = null,
    private val onSettingClick: ((View) -> Unit)? = null,
) : DefaultLifecycleObserver {

    init {
        activity.lifecycle.addObserver(this)
        toolbar?.apply {
            setAddClickListener(onAddClick)
            setSaveClickListener(onSaveClick)
            setSettingClickListener(onSettingClick)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        toolbar = null
    }
}