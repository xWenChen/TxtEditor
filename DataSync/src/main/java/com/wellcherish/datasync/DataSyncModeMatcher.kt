package com.wellcherish.datasync

import com.wellcherish.datasync.constants.DataSyncMode
import com.wellcherish.datasync.wifi.WifiDataSyncService

object DataSyncModeMatcher {
    private val syncServiceMap = hashMapOf<DataSyncMode, IDataSyncService>().apply {
        this[DataSyncMode.WIFI] = WifiDataSyncService
    }

    fun findDataSyncServiceByMode(mode: DataSyncMode): IDataSyncService? {
        return syncServiceMap[mode]
    }
}