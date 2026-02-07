package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wellcherish.texteditor.bean.FileItem
import com.wellcherish.texteditor.model.FileChangeType
import com.wellcherish.texteditor.model.FileRepository
import com.wellcherish.texteditor.utils.ZLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val dataListLiveData = MutableLiveData<List<FileItem>>()
    private val fileRepository = FileRepository

    val onFileChanged = run@{ _: String, changeType: FileChangeType ->
        if (changeType == FileChangeType.UPDATE || changeType == FileChangeType.UNKNOWN) {
            return@run
        }
        // 重新加载数据
        loadData()
    }

    fun init() {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.Main) {
            val files = withContext(Dispatchers.IO) {
                kotlin.runCatching {
                    fileRepository.loadFiles()
                }.onFailure {
                    ZLog.e(TAG, it)
                }.getOrNull()
            }
            dataListLiveData.value = files
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}