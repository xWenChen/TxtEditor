package com.wellcherish.datasync.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wellcherish.base.log.ZLog

/**
 * 使用 wifi p2p 技术进行数据同步的类。详细教程见：https://developer.android.com/develop/connectivity/wifi/wifip2p?hl=zh-cn
 *
 * 官方的demo代码：https://android.googlesource.com/platform/development/+/refs/heads/main/samples/WiFiDirectDemo/src/com/example/android/wifidirect/WiFiDirectActivity.java
 *
 * 对于以 Android 13（API 级别 33）及更高版本为目标平台的应用，
 * discoverPeers() 和 connect() 都需要 android.permission.NEARBY_WIFI_DEVICES 权限。
 * 对于以较低版本的 Android 为目标平台的应用，这些方法需要 ACCESS_FINE_LOCATION 权限。
 * 由于这些权限属于危险权限，因此您必须在运行时请求这些权限，然后才能调用 discoverPeers() 或 connect()。
 *
 * 除了上面的权限外，以下 API 还需要启用位置信息模式：
 * - discoverPeers()
 * - discoverServices()
 * - requestPeers()
 *
 * -
 * */
class WifiDataSyncManager(private var activity: AppCompatActivity?): DefaultLifecycleObserver {

    private var manager: WifiP2pManager? = null

    /**
     * Channel用于将应用连接到 Wi-Fi P2P 框架
     * */
    private var channel: WifiP2pManager.Channel? = null
    private var listener: WifiP2pManager.ChannelListener? = null
    private var receiver: BroadcastReceiver? = null
    var isWifiP2pEnabled = MutableLiveData(false)
    var retryChannel = false

    /**
     * onCreate时调用该方法。
     * */
    fun init(lifecycle: Lifecycle): Boolean {
        if (!initP2p()) {
            return false
        }
        lifecycle.addObserver(this)
        return true
    }

    private fun initP2p(): Boolean {
        // Device capability definition check
        if (activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT) != true) {
            ZLog.e(TAG, "Wi-Fi Direct is not supported by this device.")
            return false
        }
        val wifiManager = activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            ZLog.e(TAG, "Cannot get Wi-Fi system service.")
            return false
        }
        if (!wifiManager.isP2pSupported) {
            ZLog.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.")
            return false
        }
        manager = activity?.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        if (manager == null) {
            ZLog.e(TAG, "Cannot get Wi-Fi Direct system service.")
            return false
        }
        channel = manager?.initialize(activity, Looper.getMainLooper(), null);
        if (channel == null) {
            ZLog.e(TAG, "Cannot initialize Wi-Fi Direct.")
            return false
        }
        return true
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        channel?.let { channel ->
            receiver = WiFiDirectBroadcastReceiver(manager, channel, ::updateDeviceList, ::updateWifiP2PEnableState)
            // 注册广播监听
            receiver?.also { activity?.registerReceiver(it, intentFilter) }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        receiver?.also { activity?.unregisterReceiver(it) }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        activity = null
        channel = null
        listener = null
    }

    /**
     * 处理配对设备的结果回调。
     * */
    private fun updateDeviceList(peersList: WifiP2pDeviceList?) {
        // Handle peers list
    }

    /**
     * 处理Wifi P2P enable状态变更。
     * */
    private fun updateWifiP2PEnableState(enable: Boolean) {
        isWifiP2pEnabled.postValue(enable)
        if (!enable) {
            // todo activity.resetData()
        }
    }

    fun startDataSync() {
        val activity = activity ?: return
        val syncRequest = OneTimeWorkRequestBuilder<FileTransferWorker>()
            //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // 关键点：开启加急模式
            .build()

        WorkManager.getInstance(activity).enqueue(syncRequest)
    }

    companion object {
        private const val TAG = "WifiDataSyncManager"

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        /**
         * WIFI P2P 需要检查位置权限。
         * */
        fun AppCompatActivity.checkWifiP2PPermission(onResult: (Boolean) -> Unit) {
            val permission = Manifest.permission.ACCESS_FINE_LOCATION
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                // 已有权限
                onResult(true)
                return
            }
            // 申请
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                onResult(isGranted)
            }.launch(permission)
        }
    }
}