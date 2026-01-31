package com.wellcherish.texteditor.utils

import android.util.Log

object ZLog {
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    fun e(tag: String, throwable: Throwable) {
        Log.e(tag, "", throwable)
    }

    fun e(tag: String, msg: String, throwable: Throwable) {
        Log.e(tag, msg, throwable)
    }
}