package com.wellcherish.datasync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.wellcherish.base.log.ZLog
import com.wellcherish.datasync.constants.DataSyncMode

/**
 * 数据同步管理器。
 *
 * 用法：
 * 1. 先调用 [setSyncWay] 设置同步方式（指定 UI 上下文，用于后续创建服务）。
 * 2. 再通过 [currentService] 获取 [IDataSyncService] 进行传输。
 * */
object DataSyncManager : DefaultLifecycleObserver {

    private const val TAG = "DataSyncManager"

    private var curSyncWay = DataSyncMode.UNKNOWN
    private var appContext: android.content.Context? = null

    private var _currentService: IDataSyncService? = null
    /** 当前生效的数据同步服务。 */
    val currentService: IDataSyncService?
        get() = _currentService

    /**
     * 设置数据同步方案。
     *
     * @param activity 用于获取应用上下文。
     * @param way 目标同步方式（如 [DataSyncMode.WIFI]）。
     * */
    fun setSyncWay(activity: AppCompatActivity, way: DataSyncMode) {
        if (way == DataSyncMode.UNKNOWN) {
            ZLog.w(TAG, "setSyncWay, way is unknown.")
            return
        }
        runCatching {
            val context = activity.applicationContext
            appContext = context
            curSyncWay = way
            _currentService = DataSyncModeMatcher.findDataSyncServiceByMode(context, way)
        }.onFailure {
            ZLog.e(TAG, it)
        }
    }

    /**
     * 释放当前数据同步服务的资源。
     * */
    fun release() {
        (_currentService as? com.wellcherish.datasync.wifi.WifiDataSyncService)?.release()
        _currentService = null
        curSyncWay = DataSyncMode.UNKNOWN
        appContext = null
    }
}
