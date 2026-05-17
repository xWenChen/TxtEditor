package com.wellcherish.datasync.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import com.wellcherish.base.log.ZLog
import com.wellcherish.datasync.ISyncManager
import com.wellcherish.datasync.constants.FindDevicesResult

/**
 * 使用 wifi p2p 技术进行数据同步的类。详细教程见：https://developer.android.com/develop/connectivity/wifi/wifip2p?hl=zh-cn
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
 * */
class WifiDataSyncManager(private var context: Context?) : ISyncManager {

    private val manager: WifiP2pManager? by lazy {
        context?.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    /**
     * Channel用于将应用连接到 Wi-Fi P2P 框架
     * */
    private var channel: WifiP2pManager.Channel? = null
    private var listener: WifiP2pManager.ChannelListener? = null
    private var receiver: BroadcastReceiver? = null

    /**
     * 发现设备的结果回调，这只是开启扫描，并不是拿到最终的结果。发现设备的结果通过广播回传，需要接收广播信号进行请求。
     * */
    private var onFindDeviceFinish: ((FindDevicesResult) -> Unit)? = null

    /**
     * 获取配对设备的结果回调。
     * */
    private var onGetDeviceList: ((WifiP2pDeviceList?) -> Unit)? = { peers ->
        // Handle peers list
    }

    /**
     * 在Activity的onCreate方法中调用。
     * */
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        val mContext = context
        if (mContext == null) {
            ZLog.w(TAG, "init, context is null.")
            return
        }
        val mChannel = manager?.initialize(mContext, Looper.getMainLooper(), null)
        val mManager = manager
        if (mManager == null || mChannel == null) {
            ZLog.w(TAG, "init, manager or channel is null.")
            return
        }
        channel = mChannel
        receiver = WiFiDirectBroadcastReceiver(mManager, mChannel, onGetDeviceList) { wifiP2PEnabled ->

        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        receiver?.also { context?.registerReceiver(it, intentFilter) }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        receiver?.also { context?.unregisterReceiver(it) }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        context = null
        channel = null
        listener = null
        onFindDeviceFinish = null
    }

    override fun start() {
        val onFindDeviceFinish = onFindDeviceFinish ?: return
        // 调用 discoverPeers() 方法开始寻找附近的设备。这只是开启扫描，结果会通过广播返回。
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // 搜索配对列表成功后，结果通过广播回传。
                onFindDeviceFinish(FindDevicesResult.Success())
            }

            override fun onFailure(reasonCode: Int) {
                onFindDeviceFinish((FindDevicesResult.Error(reasonCode)))
            }
        })
    }

    override fun onFindDeviceFinish(callback: ((FindDevicesResult) -> Unit)?): ISyncManager {
        this.onFindDeviceFinish = callback
        return this
    }

    companion object {
        private const val TAG = "WifiDataSyncManager"

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }
}