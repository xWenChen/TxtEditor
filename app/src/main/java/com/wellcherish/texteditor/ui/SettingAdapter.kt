package com.wellcherish.texteditor.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wellcherish.base.log.ZLog
import com.wellcherish.base.utils.DrawableUtils
import com.wellcherish.base.utils.setNoDoubleClickListener
import com.wellcherish.texteditor.bean.SettingItem
import com.wellcherish.texteditor.databinding.SettingItemBinding

class SettingAdapter() : ListAdapter<SettingItem, SettingViewHolder>(
    object : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val binding = SettingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(position, itemCount, item)
    }
}

class SettingViewHolder(private val binding: SettingItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(position: Int, itemCount: Int, data: SettingItem?) {
        if (data == null) {
            ZLog.e(TAG, "data=null, pos:$position")
            return
        }
        binding.tvText.text = data.text
        binding.content.apply {
            background = DrawableUtils.randomBg()
            // 必须设置 Clickable 才能触发水波纹效果
            isClickable = true
            isFocusable = true
            setNoDoubleClickListener {
                data.noDoubleClick(it, position, data)
            }
        }
    }

    companion object {
        private const val TAG = "FileViewHolder"
    }
}