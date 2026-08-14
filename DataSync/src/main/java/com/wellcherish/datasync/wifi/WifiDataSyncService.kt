package com.wellcherish.datasync.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.wellcherish.base.log.ZLog
import com.wellcherish.datasync.IDataSyncService
import com.wellcherish.datasync.bean.TransferProgress
import com.wellcherish.datasync.bean.TransferState
import com.wellcherish.datasync.wifi.WifiP2pStateMediator.ConnectionState
import com.wellcherish.datasync.wifi.transfer.P2pDataReceiver
import com.wellcherish.datasync.wifi.transfer.P2pDataSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 使用 WiFi P2P 同步数据的服务实现。
 *
 * 发送方(客户端)：discoverPeers → requestPeers 选设备 → connect → 作为 Socket 客户端发送。
 * 接收方(Group Owner)：createGroup 成为 GO → ServerSocket 监听 → 接收。
 * */
internal class WifiDataSyncService(
    private val context: Context
) : IDataSyncService {

    private val TAG = "WifiDataSyncService"

    // ---- 状态流 ----
    private val _stateFlow = MutableStateFlow<TransferState>(TransferState.Idle)
    override val stateFlow: StateFlow<TransferState> = _stateFlow.asStateFlow()

    // ---- WiFi P2P 系统服务 ----
    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    private var coroutineP2pManager: CoroutineP2pManager? = null
    private var stateMediator: WifiP2pStateMediator? = null

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
     * 检查权限是否已全部授予。动态申请需由调用方(UI 层)通过 Activity 完成，
     * 这里仅做静态检查；若未授予则返回 false。
     */
    private fun hasAllPermissions(): Boolean {
        return requestedPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 初始化 WiFi P2P Manager，并注册广播。
     */
    private fun initWifiP2p(): Boolean {
        val manager = wifiP2pManager ?: return false
        val looper = context.getSystemService(Context.WIFI_P2P_SERVICE)?.let { _ -> Looper.getMainLooper() }
        val ch = manager.initialize(context, looper, null) ?: return false
        channel = ch
        coroutineP2pManager = CoroutineP2pManager(manager, ch)

        val mediator = WifiP2pStateMediator(manager, ch)
        stateMediator = mediator
        receiver = mediator.createReceiver()
        return true
    }

    /**
     * 注册广播接收器。
     */
    fun registerReceiver() {
        val r = receiver ?: return
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        runCatching { context.registerReceiver(r, filter) }
    }

    /**
     * 注销广播接收器。
     */
    fun unregisterReceiver(r: BroadcastReceiver? = receiver) {
        r ?: return
        runCatching { context.unregisterReceiver(r) }
        receiver = null
    }

    override fun sendFiles(targets: List<File>): Flow<TransferProgress> = flow<TransferProgress> {
        if (!hasAllPermissions()) {
            _stateFlow.value = TransferState.Failed("缺少必要权限（需在UI层动态申请）")
            return@flow
        }
        if (channel == null && !initWifiP2p()) {
            _stateFlow.value = TransferState.Failed("Wi-Fi P2P 初始化失败")
            return@flow
        }
        registerReceiver()
        try {
            // 1. 设备发现
            _stateFlow.value = TransferState.Discovering
            val p2p = coroutineP2pManager ?: return@flow
            val mediator = stateMediator ?: return@flow
            ZLog.d(TAG, "开始设备发现")
            p2p.discoverPeers()

            // 等待设备列表非空
            val target = mediator.deviceList.first { it.isNotEmpty() }.firstOrNull()
                ?: run {
                    _stateFlow.value = TransferState.Failed("未发现可用设备")
                    return@flow
                }

            // 2. 连接
            _stateFlow.value = TransferState.Connecting(target.deviceName)
            p2p.connect(target.deviceAddress)

            // 等待连接成功并获取 GO IP
            val goIp = waitForConnected(mediator)?.groupOwnerAddress?.hostAddress
                ?: run {
                    _stateFlow.value = TransferState.Failed("无法获取目标设备 IP")
                    return@flow
                }

            // 3. 数据传输
            val sender = P2pDataSender(goIp, P2pDataSender.DEFAULT_PORT)
            sender.send(targets).collect { progress ->
                _stateFlow.value = TransferState.Transferring(progress.relativePath, progress.progressPercent)
                emit(progress)
            }

            // 4. 传输结束
            _stateFlow.value = TransferState.Completed
        } catch (e: Exception) {
            ZLog.e(TAG, "sendFiles failed", e)
            _stateFlow.value = TransferState.Failed(e.message ?: "发送失败")
        } finally {
            unregisterReceiver()
        }
    }.flowOn(Dispatchers.IO)

    override fun receive(targetDir: File): Flow<TransferProgress> = flow {
        if (!hasAllPermissions()) {
            _stateFlow.value = TransferState.Failed("缺少必要权限（需在UI层动态申请）")
            return@flow
        }
        if (channel == null && !initWifiP2p()) {
            _stateFlow.value = TransferState.Failed("Wi-Fi P2P 初始化失败")
            return@flow
        }
        registerReceiver()
        try {
            // 1. 作为 Group Owner 创建群组
            _stateFlow.value = TransferState.Discovering
            val p2p = coroutineP2pManager ?: return@flow
            p2p.createGroup()

            // 2. 监听接收
            _stateFlow.value = TransferState.Connecting("等待组员连接")
            targetDir.mkdirs()
            val receiver = P2pDataReceiver(targetDir, P2pDataSender.DEFAULT_PORT)
            receiver.receive().collect { progress ->
                _stateFlow.value = TransferState.Transferring(progress.relativePath, progress.progressPercent)
                emit(progress)
            }

            // 3. 传输结束
            _stateFlow.value = TransferState.Completed
        } catch (e: Exception) {
            ZLog.e(TAG, "receive failed", e)
            _stateFlow.value = TransferState.Failed(e.message ?: "接收失败")
        } finally {
            unregisterReceiver()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 等待 P2P 连接建立，返回 [WifiP2pInfo]。
     * */
    private suspend fun waitForConnected(mediator: WifiP2pStateMediator): WifiP2pInfo? {
        return withContext(Dispatchers.IO) {
            var attempts = 0
            while (attempts < 50) { // 最多 5s
                val state = mediator.connectionState.value
                if (state is ConnectionState.Connected) {
                    return@withContext state.info
                }
                if (state is ConnectionState.Disconnected) {
                    return@withContext null
                }
                delay(100)
                attempts++
            }
            null
        }
    }

    /**
     * 释放资源。
     */
    fun release() {
        unregisterReceiver()
    }
}
