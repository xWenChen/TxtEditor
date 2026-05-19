package com.wellcherish.datasync.wifi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wellcherish.base.log.ZLog
import com.wellcherish.datasync.IDataSyncService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 使用wifi同步数据的管理类。流程说明：
 *
 * 1、请求权限。
 *
 * */
internal object WifiDataSyncService : IDataSyncService {
    private const val TAG = "WifiDataSyncService"

    /**
     * wifi p2p 需要的权限。
     * */
    private val requestedPermissions = buildList {
        add(Manifest.permission.INTERNET)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.CHANGE_WIFI_STATE)
        add(Manifest.permission.ACCESS_NETWORK_STATE)
        add(Manifest.permission.CHANGE_NETWORK_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * 检查并申请权限的核心方法
     */
    private suspend fun checkAndRequestPermissions(activity: AppCompatActivity?): Boolean {
        activity ?: return false
        return suspendCoroutine {
            // 过滤出未被授予的权限
            val notGrantedPermissions = requestedPermissions.filter {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (notGrantedPermissions.isEmpty()) {
                // 所有的权限都已经授予过了
                it.resume(true)
            } else {
                // 存在未授予的权限，直接调用 Launcher 动态申请
                // 系统会自动忽略已经授予的权限，只向用户弹窗请求未授予的权限
                try {
                    activity.registerForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsResult ->
                        // 这里会返回一个 Map<String, Boolean>，Key 是权限名，Value 是是否授权
                        val deniedPermissions = permissionsResult.filterValues { !it }.keys

                        if (deniedPermissions.isEmpty()) {
                            // 所有权限都被授予了！
                            it.resume(true)
                        } else {
                            // 有部分或全部权限被拒绝
                            ZLog.e(TAG, "以下权限被拒绝: $deniedPermissions")
                            // 可以在这里引导用户去设置页，或者处理权限缺失的逻辑
                            it.resume(false)
                        }
                    }.launch(notGrantedPermissions)
                } catch (e: Exception) {
                    ZLog.e(TAG, e)
                    it.resume(false)
                }
            }
        }
    }
}