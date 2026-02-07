package com.wellcherish.texteditor.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.wellcherish.texteditor.databinding.MainToolbarBinding
import com.wellcherish.texteditor.utils.setNoDoubleClickListener

class MainToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: MainToolbarBinding

    init {
        binding = MainToolbarBinding.inflate(LayoutInflater.from(context), this ,true)
    }

    fun setTitle(title: CharSequence?) {
        binding.ivAppName.text = title
    }

    fun setShowSave(isShow: Boolean) {
        binding.ivSave.isVisible = isShow
    }

    fun setShowSetting(isShow: Boolean) {
        binding.ivSetting.isVisible = isShow
    }

    fun setSaveClickListener(onClick: ((View) -> Unit)?) {
        if (onClick == null) {
            binding.ivSave.setOnClickListener(null)
        } else {
            binding.ivSave.setNoDoubleClickListener(onClick = onClick)
        }
    }

    fun setSettingClickListener(onClick: ((View) -> Unit)?) {
        if (onClick == null) {
            binding.ivSetting.setOnClickListener(null)
        } else {
            binding.ivSetting.setNoDoubleClickListener(onClick = onClick)
        }
    }
}