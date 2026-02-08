package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.model.FileChangeType
import com.wellcherish.texteditor.model.FileRepository
import com.wellcherish.texteditor.utils.ZLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val showLoading = MutableLiveData(false)
    val showEmpty = MutableLiveData(false)
    val dataListLiveData = MutableLiveData<List<FileItem>>()
    private val fileRepository = FileRepository

    val onFileChanged = run@{ _: String, changeType: FileChangeType ->
        if (changeType == FileChangeType.UNKNOWN) {
            return@run
        }
        viewModelScope.launch(Dispatchers.IO) {
            // 重新加载数据
            loadData()
        }
    }

    fun changeLoadingState(isShow: Boolean) {
        if (showLoading.value == isShow) {
            return
        }
        showLoading.postValue(isShow)
    }

    suspend fun init() {
        loadData()
    }

    private suspend fun loadData() {
        val files = withContext(Dispatchers.IO) {
            kotlin.runCatching {
                fileRepository.loadNotDeletedFiles()
            }.onFailure {
                ZLog.e(TAG, it)
            }.getOrNull()
        }
        dataListLiveData.postValue(files)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}