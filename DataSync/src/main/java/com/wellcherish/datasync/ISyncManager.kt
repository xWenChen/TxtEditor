package com.wellcherish.datasync

import androidx.lifecycle.DefaultLifecycleObserver
import com.wellcherish.datasync.constants.FindDevicesResult

interface ISyncManager : DefaultLifecycleObserver {
    fun onFindDeviceFinish(callback: ((FindDevicesResult) -> Unit)?): ISyncManager
    fun start()
}