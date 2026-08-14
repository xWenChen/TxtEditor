package com.wellcherish.datasync.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 监听 Wi-Fi P2P 系统广播，并派发到状态流中。
 *
 * - [WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION]：触发重新获取设备列表
 * - [WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION]：连接/断开事件
 * - [WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION]：Wi-Fi P2P 硬件启用/禁用
 * */
internal class WifiP2pStateMediator(
    private val manager: WifiP2pManager,
    private val channel: Channel
) {

    private val _deviceList = MutableStateFlow<List<DeviceInfo>>(emptyList())
    /** 最近一次扫描到的可用设备列表。 */
    val deviceList: StateFlow<List<DeviceInfo>> = _deviceList.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    /** P2P 连接状态流。 */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val info: WifiP2pInfo) : ConnectionState()
        data class Disconnected(val message: String = "连接已断开") : ConnectionState()
    }

    /** 设备基本信息，隐藏底层 [WifiP2pDeviceList]。 */
    data class DeviceInfo(val deviceName: String, val deviceAddress: String) {
        override fun toString(): String = deviceName
    }

    /**
     * 创建并返回广播接收器。
     * */
    fun createReceiver(): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // 设备列表发生变化，向系统请求最新设备列表
                    manager.requestPeers(channel) { deviceList ->
                        _deviceList.value = deviceList.deviceList.map {
                            DeviceInfo(it.deviceName, it.deviceAddress)
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(
                        WifiP2pManager.EXTRA_NETWORK_INFO
                    )
                    val wifiP2pInfo = intent.getParcelableExtra<WifiP2pInfo>(
                        WifiP2pManager.EXTRA_WIFI_P2P_INFO
                    )
                    if (networkInfo?.isConnected == true && wifiP2pInfo != null) {
                        _connectionState.value = ConnectionState.Connected(wifiP2pInfo)
                    } else {
                        _connectionState.value = ConnectionState.Disconnected()
                    }
                }

                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(
                        WifiP2pManager.EXTRA_WIFI_STATE,
                        WifiP2pManager.WIFI_P2P_STATE_DISABLED
                    )
                    if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED &&
                        _connectionState.value is ConnectionState.Connected
                    ) {
                        _connectionState.value = ConnectionState.Disconnected("Wi-Fi P2P 已关闭")
                    }
                }
            }
        }
    }
}
