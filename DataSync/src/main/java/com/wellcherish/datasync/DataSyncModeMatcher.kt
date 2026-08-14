package com.wellcherish.datasync

import android.content.Context
import com.wellcherish.datasync.constants.DataSyncMode
import com.wellcherish.datasync.wifi.WifiDataSyncService

object DataSyncModeMatcher {

    /**
     * 根据模式创建对应的数据同步服务（工厂）。
     *
     * @param context 应用上下文（app context）
     * @param mode 同步方式
     * @return 匹配到的服务实例；若无匹配返回 null
     * */
    fun findDataSyncServiceByMode(context: Context, mode: DataSyncMode): IDataSyncService? {
        return when (mode) {
            DataSyncMode.WIFI -> WifiDataSyncService(context.applicationContext)
            else -> null
        }
    }
}
