package com.wellcherish.texteditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.wellcherish.base.log.ZLog
import com.wellcherish.base.ui.IView
import com.wellcherish.base.utils.stringRes
import com.wellcherish.texteditor.R
import com.wellcherish.texteditor.bean.SettingItem
import com.wellcherish.texteditor.model.SettingType

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    private var view: IView? = null

    val showLoading = MutableLiveData(false)
    val showEmpty = MutableLiveData(false)
    val dataListLiveData = MutableLiveData<List<SettingItem>>()

    fun changeLoadingState(isShow: Boolean) {
        if (showLoading.value == isShow) {
            return
        }
        showLoading.postValue(isShow)
    }

    fun init(view: IView) {
        this.view = view
        loadData()
    }

    fun onDestroy() {
        this.view = null
    }

    private fun loadData() {
        val settingList = kotlin.runCatching {
            getDataList()
        }.onFailure {
            ZLog.e(TAG, it)
        }.getOrNull()
        dataListLiveData.postValue(settingList)
    }

    private fun getDataList(): List<SettingItem> {
        return mutableListOf<SettingItem>().apply {
            add(SettingItem(SettingType.DATA_SYNC, R.string.trans_data.stringRes) {_, position, data ->
                // todo：跳转页面
            })
            add(SettingItem(SettingType.ABOUT, R.string.about.stringRes) {_, position, data ->
                // todo：跳转页面
            })
        }
    }

    companion object {
        private const val TAG = "SettingViewModel"
    }
}