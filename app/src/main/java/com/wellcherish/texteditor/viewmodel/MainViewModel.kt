package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.config.DefaultConfig
import com.wellcherish.texteditor.utils.*
import com.wellcherish.txteditor.R
import kotlinx.coroutines.*
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val autoSaveExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        ZLog.e(TAG, throwable)
    }
    private var autoSaveJob: Job? = null
    /**
     * 文本内容的保存状态
     * */
    val contentSaveState = MutableLiveData(SaveState.SAVED)
    /**
     * 当前被打开文件的标题。会被用作文件名。文件名的生成规则见：[com.wellcherish.texteditor.utils.getFileName]
     * */
    var currentOpenTxtFileTitle: CharSequence? = null
    /**
     * 当前正在被编辑的Txt文件。
     * */
    var currentOpenTxtFile: File? = null

    fun changeContentSaveState(newState: SaveState) {
        if (contentSaveState.value == newState) {
            return
        }
        contentSaveState.postValue(newState)
    }

    /**
     * 协程开启轮询任务，进行自动保存。
     * */
    fun startAutoSave(getInputText: (() -> CharSequence?)) {
        autoSaveJob = viewModelScope.launch(Dispatchers.IO + autoSaveExceptionHandler) {
            while (isActive) {
                delay(ConfigManager.autoSaveDuration)
                saveText(getInputText())
            }
        }
    }

    /**
     * 保存EditText的文件内容
     * */
    private suspend fun saveText(newText: CharSequence?) {
        withContext(Dispatchers.autoSave) {
            val content = newText?.toString() ?: ""
            val file = currentOpenTxtFile ?: run {
                // 当前没有打开的文件，尝试创建一个新文件
                val fileName = getFileName(currentOpenTxtFileTitle ?: R.string.default_title_name.stringRes)

            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (autoSaveJob?.isActive == true) {
            autoSaveJob?.cancel()
        }
        autoSaveJob = null
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}