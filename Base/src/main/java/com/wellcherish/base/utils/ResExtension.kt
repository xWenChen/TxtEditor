package com.wellcherish.base.utils

import androidx.core.content.ContextCompat
import com.wellcherish.base.MainApplication

val Int.colorRes: Int
    get() = ContextCompat.getColor(MainApplication.context, this)

val Int.stringRes: String
    get() = MainApplication.context.getString(this)

val Int.dimenRes: Int
    get() = MainApplication.context.resources.getDimensionPixelSize(this)