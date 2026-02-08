package com.wellcherish.texteditor.mainlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.databinding.TextFileItemBinding
import com.wellcherish.texteditor.utils.ZLog
import com.wellcherish.texteditor.utils.safeTitle
import com.wellcherish.texteditor.utils.setNoDoubleClickListener

class MainAdapter(private val noDoubleClick: (View, Int, FileItem) -> Unit) : ListAdapter<FileItem, FileViewHolder>(
    object : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.contentId == newItem.contentId
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = TextFileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding, noDoubleClick)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(position, item)
    }
}

class FileViewHolder(
    private val binding: TextFileItemBinding,
    private val noDoubleClick: (View, Int, FileItem) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(position: Int, data: FileItem?) {
        if (data == null) {
            ZLog.e(TAG, "data=null, pos:$position")
            return
        }
        binding.tvTitle.text = data.title.safeTitle()
        binding.tvContent.text = data.text
        binding.root.setNoDoubleClickListener {
            noDoubleClick(it, position, data)
        }
    }

    companion object {
        private const val TAG = "FileViewHolder"
    }
}