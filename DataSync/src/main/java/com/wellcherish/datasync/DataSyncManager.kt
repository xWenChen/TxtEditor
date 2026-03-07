package com.wellcherish.datasync

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.wellcherish.base.log.ZLog
import com.wellcherish.datasync.constants.DataSyncWay
import com.wellcherish.datasync.wifi.WifiDataSyncManager

/**
 * 先调用 setSyncWay，再注册observer。
 * */
object DataSyncManager : DefaultLifecycleObserver {
    private const val TAG = "DataSyncManager"
    private var curSyncWay = DataSyncWay.UNKNOWN
    private var syncManagerDelegate: ISyncManager? = null

    fun init(activity: AppCompatActivity, way: DataSyncWay) {
        setSyncWay(activity, way)
        activity.lifecycle.addObserver(this)
    }

    /**
     * 设置数据同步方案
     * */
    fun setSyncWay(activity: AppCompatActivity, way: DataSyncWay) {
        runCatching {
            syncManagerDelegate?.let { activity.lifecycle.removeObserver(it) }
            when (curSyncWay) {
                DataSyncWay.WIFI -> {
                    // 取消现有同步，重新开始同步。
                    syncManagerDelegate = WifiDataSyncManager(activity).apply {
                        activity.lifecycle.addObserver(this)
                        start()
                    }
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