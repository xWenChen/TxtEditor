package com.wellcherish.datasync.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager

/**
 * Wi-Fi p2p 事件的广播接收器.
 *
 * - WIFI_P2P_STATE_CHANGED_ACTION: 指示是否启用 WLAN 直连。
 * - WIFI_P2P_PEERS_CHANGED_ACTION: 指示可用的对等设备列表已更改。
 * - WIFI_P2P_CONNECTION_CHANGED_ACTION: 指示 Wi-Fi Direct 连接的状态已更改。从 Android 10 开始，这不是固定的(是非粘性 intent)。
 *    如果应用依赖于在注册时接收这些广播（因为其之前一直是固定的），请在初始化时使用适当的 get 方法获取信息。
 *    应用可以使用 requestConnectionInfo()、requestNetworkInfo() 或 requestGroupInfo() 检索当前连接信息。
 * - WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: 指示此设备的配置详细信息已更改。从 Android 10 开始，这不是固定的(是非粘性 intent)。
 *    如果应用依赖于在注册时接收这些广播（因为其之前一直是固定的），请在初始化时使用适当的 get 方法获取信息。
 *    应用可以使用 requestDeviceInfo() 检索当前连接信息。
 */
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager?,
    private val channel: WifiP2pManager.Channel,
    private val onGetDeviceList: ((WifiP2pDeviceList?) -> Unit)?,
    private val onWifiP2PStateChanged: (Boolean) -> Unit,
) : BroadcastReceiver() {
   override fun onReceive(context: Context, intent: Intent) {
       when (intent.action) {
           WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
               // 指示是否启用 WLAN 直连。检查wifi是否可用。
               checkWifiP2PEnable(intent)
           }
           WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
               // 调用WifiP2pManager.requestPeers()获取可配对列表。
               manager?.requestPeers(channel) { peers ->
                   onGetDeviceList?.invoke(peers)
               }
           }
           WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
               // 响应连接或者断开动作。应用可以使用 requestConnectionInfo()、requestNetworkInfo() 或 requestGroupInfo() 检索当前连接信息。
           }
           WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
               // 响应本设备的wifi状态变更。应用可以使用 requestDeviceInfo() 检索当前连接信息。
           }
       }
   }

    private fun checkWifiP2PEnable(intent: Intent) {
        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
        val enable = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
        onWifiP2PStateChanged(enable)
    }
}