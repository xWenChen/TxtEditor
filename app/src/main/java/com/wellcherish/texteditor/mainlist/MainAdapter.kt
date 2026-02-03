package com.wellcherish.texteditor.mainlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wellcherish.texteditor.bean.FileItem
import com.wellcherish.texteditor.databinding.TextFileItemBinding
import com.wellcherish.texteditor.utils.ZLog
import com.wellcherish.texteditor.utils.safeTitle

class MainAdapter : ListAdapter<FileItem, FileViewHolder>(
    object : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder(TextFileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(position, item)
    }
}

class FileViewHolder(val binding: TextFileItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(position: Int, data: FileItem?) {
        if (data == null) {
            ZLog.e(TAG, "data=null, pos:$position")
            return
        }
        binding.tvTitle.text = data.title.safeTitle()
        binding.tvContent.text = data.text
    }

    companion object {
        private const val TAG = "FileViewHolder"
    }
}