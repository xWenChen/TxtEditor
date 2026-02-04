package com.wellcherish.texteditor.utils

import android.view.View

fun View.setNoDoubleClickListener(duration: Long = 500L, onClick: (View) -> Unit) {
    var lastClickTime = 0L

    this.setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= duration) {
            lastClickTime = currentTime
            onClick(it)
        }
    }
}