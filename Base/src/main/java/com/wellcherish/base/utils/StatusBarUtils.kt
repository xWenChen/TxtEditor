package com.wellcherish.base.utils

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

object StatusBarUtils {
    /**
     * 设置状态栏的颜色
     * */
    fun setStatusBarCustom(activity: Activity, color: Int, isLightMode: Boolean) {
        val window = activity.window

        // 设置状态栏背景色
        window.statusBarColor = color

        // 设置状态栏图标/文字颜色
        val decorView = window.decorView
        val controller = WindowCompat.getInsetsController(window, decorView)
        // isAppearanceLightStatusBars 为 true 时，图标变深色（适合浅色背景），为 false 时，图标变白色（适合深色背景）
        setLightMode(controller, isLightMode)
    }

    private fun setLightMode(controller: WindowInsetsControllerCompat?, isLightMode: Boolean) {
        controller?.isAppearanceLightStatusBars = isLightMode
    }
}