package com.wellcherish.base.utils

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import com.wellcherish.base.R
import java.util.*


object DrawableUtils {
    private const val TAG = "DrawableUtils"

    private val bgList = mutableListOf<Int>().apply {
        add(R.color.rainbow_blue.colorRes)
        add(R.color.deep_pink.colorRes)
        add(R.color.deep_green.colorRes)
        add(R.color.light_purple.colorRes)
        add(R.color.light_orange.colorRes)
    }

    private val random = Random()

    fun randomBg(): Drawable {
        // 取 0 到 bgList.size - 1 的随机数。
        val startIndex = random.nextInt(bgList.size)
        var endIndex = random.nextInt(bgList.size)
        if (startIndex == endIndex) {
            endIndex = (startIndex + 1) % bgList.size
        }
        return gradientBg(bgList[startIndex], bgList[endIndex])
    }

    fun gradientBg(
        startColor: Int,
        endColor: Int,
        radius: Int = R.dimen.padding_large.dimenRes,
    ): Drawable {
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            // 设置圆角 (Corners)
            cornerRadius = radius.toFloat()
        }
        // 1. 定义点击时的水波纹颜色（通常是半透明灰色或白色）
        val rippleColor = ColorStateList.valueOf(R.color.black_30.colorRes)

        // 2. 创建 RippleDrawable
        // 参数 1: 水波纹颜色
        // 参数 2: 内容层 (Content)，即你的渐变色背景
        // 参数 3: 遮罩层 (Mask)，决定水波纹扩散的范围（传入 gradient 即可限制在圆角矩形内）
        return RippleDrawable(rippleColor, gradient, gradient)
    }
}