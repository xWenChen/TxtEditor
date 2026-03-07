package com.wellcherish.texteditor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wellcherish.texteditor.bean.FileData
import com.wellcherish.texteditor.databinding.TextFileItemBinding
import com.wellcherish.base.log.ZLog
import com.wellcherish.base.utils.DrawableUtils
import com.wellcherish.texteditor.utils.safeTitle
import com.wellcherish.texteditor.utils.setNoDoubleClickListener

class MainAdapter(
    private val noDoubleClick: (View, Int, FileData) -> Unit,
    private val onLongClick: (View, Int, FileData) -> Unit,
    private val deleteIconClick: (View, Int, FileData) -> Unit,
) : ListAdapter<FileData, FileViewHolder>(
    object : DiffUtil.ItemCallback<FileData>() {
        override fun areItemsTheSame(oldItem: FileData, newItem: FileData): Boolean {
            return oldItem.dbData?.contentId == newItem.dbData?.contentId
        }

        override fun areContentsTheSame(oldItem: FileData, newItem: FileData): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = TextFileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding, noDoubleClick, onLongClick, deleteIconClick)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(position, itemCount, item)
    }
}

class FileViewHolder(
    private val binding: TextFileItemBinding,
    private val noDoubleClick: (View, Int, FileData) -> Unit,
    private val onLongClick: (View, Int, FileData) -> Unit,
    private val deleteIconClick: (View, Int, FileData) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(position: Int, itemCount: Int, data: FileData?) {
        if (data == null) {
            ZLog.e(TAG, "data=null, pos:$position")
            return
        }
        binding.tvTitle.text = data.dbData?.title.safeTitle()
        binding.tvContent.text = data.text
        binding.content.apply {
            background = DrawableUtils.randomBg()
            // 必须设置 Clickable 才能触发水波纹效果
            isClickable = true
            isFocusable = true
            setNoDoubleClickListener {
                if (data.showDelete) {
                    // 删除按钮展示时，不响应item的点击。
                    return@setNoDoubleClickListener
                }
                noDoubleClick(it, position, data)
            }
            setOnLongClickListener {
                onLongClick(it, position, data)
                true
            }
        }
        binding.ivDelete.setNoDoubleClickListener {
            deleteIconClick(it, position, data)
        }
        binding.ivDelete.isVisible = data.showDelete
    }

    companion object {
        private const val TAG = "FileViewHolder"
    }
}