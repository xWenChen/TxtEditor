package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.utils.*
import com.wellcherish.texteditor.R
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val autoSaveExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        ZLog.e(TAG, throwable)
    }
    private var autoSaveJob: Job? = null
    /**
     * 文本内容的保存状态
     * */
    val contentSaveState = MutableLiveData(SaveState.SAVED)
    /**
     * 当前正在被编辑的Txt文件。
     * */
    var currentOpenTxtFile: File? = null

    fun changeContentSaveState(newState: SaveState?) {
        if (contentSaveState.value == newState) {
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            contentSaveState.value = newState
        }
    }

    /**
     * 协程开启轮询任务，进行周期性的自动保存。
     * */
    fun startAutoSave(
        getTitle: (() -> CharSequence?),
        getInputText: (() -> CharSequence?),
        onFailed: () -> Unit
    ) {
        autoSaveJob = viewModelScope.launch(Dispatchers.IO + autoSaveExceptionHandler) {
            while (isActive) {
                delay(ConfigManager.autoSaveDuration)
                withContext(Dispatchers.autoSave) {
                    saveText(getTitle(), getInputText(), onFailed)
                }
            }
        }
    }

    /**
     * 保存EditText的文件内容
     * */
    fun saveText(title: CharSequence?, newText: CharSequence?, onFailed: () -> Unit) {
        val oldState = contentSaveState.value
        if (oldState == SaveState.SAVED) {
            ZLog.e(TAG, "text saved.")
            return
        }
        changeContentSaveState(SaveState.SAVING)
        val content = newText?.toString() ?: ""
        var file = currentOpenTxtFile
        if (file == null) {
            // 当前没有打开的文件，尝试创建一个新文件
            createNewFile(title).let {
                file = it
                currentOpenTxtFile = it
            }
            if (file == null) {
                ZLog.e(TAG, "saveText, create new file failed.")
                onFailed()
                changeContentSaveState(oldState)
                return
            }
        }
        runCatching {
            val oldContent = file.content()
            FileWriter(file).use { it.write(content) }
            changeContentSaveState(SaveState.SAVED)
        }.onFailure {
            ZLog.e(TAG, it)
            changeContentSaveState(oldState)
            onFailed()
        }
    }

    private fun createNewFile(title: CharSequence?): File? {
        val dir = getSaveDir()
        if (dir == null) {
            ZLog.e(TAG, "saveText, dir is null")
            return null
        }
        val fileName = getFileName(title.safeTitle())
        return runCatching {
            val newFile = File(getSaveDir(), fileName)
            if (!newFile.exists()) {
                newFile.createNewFile()
            }
            newFile
        }.onFailure {
            ZLog.e(TAG, it)
        }.getOrNull()
    }

    override fun onCleared() {
        super.onCleared()
        if (autoSaveJob?.isActive == true) {
            autoSaveJob?.cancel()
        }
        autoSaveJob = null
    }

    companion object {
        private const val TAG = "EditorViewModel"
    }
}