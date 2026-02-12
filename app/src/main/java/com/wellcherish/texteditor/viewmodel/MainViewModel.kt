package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wellcherish.texteditor.bean.FileData
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.model.FileChangeType
import com.wellcherish.texteditor.model.FileRepository
import com.wellcherish.texteditor.utils.DeleteFileUtil
import com.wellcherish.texteditor.utils.ZLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val showLoading = MutableLiveData(false)
    val showEmpty = MutableLiveData(false)
    /**
     * 是否是删除中的状态，如果是，则每个item展示删除按钮。
     * */
    var isDeleting = false
    val dataListLiveData = MutableLiveData<List<FileData>>()
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

    fun changeDeletingState(isDeleting: Boolean) {
        if (this.isDeleting == isDeleting) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            this@MainViewModel.isDeleting = isDeleting
            val list = dataListLiveData.value?.map { it.copy(showDelete = isDeleting) }
            dataListLiveData.postValue(list)
        }
    }

    fun deleteItem(position: Int, data: FileData) {
        val list = dataListLiveData.value
        if (list.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            // 文件引入应用回收站目录。数据库数据标记为删除态。
            if (!DeleteFileUtil.recycleFile(data)) {
                return@launch
            }
            val newList = mutableListOf<FileData>()
            for (item in list) {
                if (item.dbData?.contentId == data.dbData?.contentId) {
                    // 是被删除的数据，则不加入新列表。
                    continue
                }
                newList.add(item.copy())
            }
            dataListLiveData.postValue(newList)
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