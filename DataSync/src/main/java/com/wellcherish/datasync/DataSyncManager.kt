package com.wellcherish.datasync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.wellcherish.base.log.ZLog
import com.wellcherish.datasync.constants.DataSyncMode

/**
 * 先调用 setSyncWay，再注册observer。
 * */
object DataSyncManager : DefaultLifecycleObserver {
    private const val TAG = "DataSyncManager"
    private var curSyncWay = DataSyncMode.UNKNOWN

    /**
     * 设置数据同步方案
     * */
    fun setSyncWay(activity: AppCompatActivity, way: DataSyncMode) {
        runCatching {
            when (curSyncWay) {
                DataSyncMode.WIFI -> {
                    curSyncWay = way
                }
                else -> {
                    ZLog.w(TAG, "setSyncWay, way is unknown.")
                }
            }
        }.onFailure {
            ZLog.e(TAG, it)
        }
    }
}