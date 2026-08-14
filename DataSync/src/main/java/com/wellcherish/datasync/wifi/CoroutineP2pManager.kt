package com.wellcherish.datasync.wifi

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 将异步回调的 [WifiP2pManager] 方法线性化封装为 suspend 函数。
 *
 * 所有方法均保持挂起语义，调用方无需关心底层回调与线程切换。
 * */
internal class CoroutineP2pManager(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel
) {

    /**
     * 启动设备发现。
     * */
    @SuppressLint("MissingPermission")
    suspend fun discoverPeers(): Boolean = suspendCancellableCoroutine { continuation ->
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onFailure(reason: Int) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        RuntimeException("设备发现失败，errorCode=$reason")
                    )
                }
            }
        })
    }

    /**
     * 请求当前发现的设备列表。
     * */
    @SuppressLint("MissingPermission")
    suspend fun requestPeers(): WifiP2pDeviceList = suspendCancellableCoroutine { continuation ->
        manager.requestPeers(channel) { deviceList ->
            if (continuation.isActive) continuation.resume(deviceList)
        }
    }

    /**
     * 与目标设备建立连接。
     *
     * @param deviceAddress 目标设备地址
     * */
    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Boolean = suspendCancellableCoroutine { continuation ->
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            groupOwnerIntent = 0 // 请求作为 Group Client 加入
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onFailure(reason: Int) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        RuntimeException("建立P2P连接失败，errorCode=$reason")
                    )
                }
            }
        })
    }

    /**
     * 创建 P2P 群组，本机成为 Group Owner。
     * */
    @SuppressLint("MissingPermission")
    suspend fun createGroup(): Boolean = suspendCancellableCoroutine { continuation ->
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onFailure(reason: Int) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        RuntimeException("创建P2P群组失败，errorCode=$reason")
                    )
                }
            }
        })
    }

    /**
     * 移除当前 P2P 群组。
     * */
    @SuppressLint("MissingPermission")
    suspend fun removeGroup(): Boolean = suspendCancellableCoroutine { continuation ->
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onFailure(reason: Int) {
                // 没有群组时返回 BUSY，视为已断开
                if (reason == WifiP2pManager.ERROR || reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            RuntimeException("移除P2P群组失败，errorCode=$reason")
                        )
                    }
                } else {
                    if (continuation.isActive) continuation.resume(true)
                }
            }
        })
    }

    /**
     * 请求当前连接信息，包含 Group Owner 的 IP 等。
     * */
    @SuppressLint("MissingPermission")
    suspend fun requestConnectionInfo(): WifiP2pInfo? = suspendCancellableCoroutine { continuation ->
        manager.requestConnectionInfo(channel) { info ->
            if (continuation.isActive) continuation.resume(info)
        }
    }
}
