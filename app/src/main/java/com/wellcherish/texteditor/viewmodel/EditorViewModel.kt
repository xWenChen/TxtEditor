package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.wellcherish.texteditor.config.ConfigManager
import com.wellcherish.texteditor.database.bean.FileItem
import com.wellcherish.texteditor.model.FileChangeType
import com.wellcherish.texteditor.model.FileEventBus
import com.wellcherish.texteditor.model.FileRepository
import com.wellcherish.texteditor.utils.*
import com.wellcherish.texteditor.model.SaveState
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
    var currentOpenFile: FileItem? = null

    /**
     * 文件变更的类型，每次保存成功后，才会重置状态。防止异常场景下状态出错。
     * */
    var fileChangeType = FileChangeType.UNKNOWN

    fun onDestroy() {
        changeContentSaveState(SaveState.SAVED)
        currentOpenFile = null
        fileChangeType = FileChangeType.UNKNOWN
    }

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
        getTitle: (() -> String?),
        getInputText: (() -> String),
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
     * 保存EditText的文件内容。先把内容保存到数据库，再把内容写入文件。
     * */
    fun saveText(title: String?, newText: String, onFailed: () -> Unit) {
        val oldState = contentSaveState.value
        // 已经保存过了，不必重复保存。
        if (oldState == SaveState.SAVED) {
            ZLog.e(TAG, "text saved.")
            return
        }
        runCatching {
            changeContentSaveState(SaveState.SAVING)
            if (fileChangeType == FileChangeType.UNKNOWN) {
                fileChangeType = FileChangeType.UPDATE
            }
            val file = getOrCreateFileData(title, newText, oldState, onFailed)
            val finalFile = file?.filePath?.let { File(it) } ?: return
            val oldContent = finalFile.content()
            if (oldContent == newText) {
                return@runCatching
            }
            FileWriter(finalFile).use { it.write(newText) }
            changeContentSaveState(SaveState.SAVED)
            // 通知文件发生变更。
            FileEventBus.notifyFileChanged(finalFile.absolutePath, fileChangeType)
            // 保存成功，重置状态。
            fileChangeType = FileChangeType.UNKNOWN
        }.onFailure {
            ZLog.e(TAG, it)
            changeContentSaveState(oldState)
            onFailed()
        }
    }

    /**
     * 此方法会更新title
     * */
    private fun getOrCreateFileData(
        title: String?,
        newText: String,
        oldState: SaveState?,
        onFailed: () -> Unit
    ): FileItem? {
        var file = currentOpenFile
        // 没有文件，或者标题变了，都需要新建文件。
        if (file == null || file.title.isNullOrEmpty() || file.title != title) {
            // 标题可能变化了，创建一个新文件，用于对比。
            val newFile = createNewFile(title)
            if (newFile == null) {
                ZLog.e(TAG, "saveText, create new file failed.")
                onFailed()
                changeContentSaveState(oldState)
                return null
            }
            // 当前没有打开的文件，尝试创建一个新文件
            file = FileItem(
                file?.id ?: 0L,
                file?.contentId ?: generateContentId(),
                newFile.absolutePath,
                newFile.getFileTitle(),
                newFile.lastModified(),
                file?.isDeleted ?: false,
                newText
            )

            // 数据保存到db
            FileRepository.updateOrInsertDbData(file)

            // 刪除旧文件，保留新文件。
            currentOpenFile?.filePath?.let {
                File(it).delete()
            }
            // 创建了新文件，需要删除旧文件
            currentOpenFile = file
            // 文件变更状态为新增。
            fileChangeType = FileChangeType.ADDED
        } else {
            // 直接更新数据库
            file.updateTime = System.currentTimeMillis()
            // 数据保存到db
            FileRepository.updateOrInsertDbData(file)
        }
        return file
    }

    private fun createNewFile(title: String?): File? {
        val dir = getSaveDir()
        if (dir == null) {
            ZLog.e(TAG, "saveText, dir is null")
            return null
        }
        val fileName = getFileName(title ?: "")
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