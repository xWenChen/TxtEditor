package com.wellcherish.datasync.bean

/**
 * 传输生命周期状态机。
 *
 * 覆盖：设备发现(Discovering) → 设备连接(Connecting) → 数据传输(Transferring) → 传输结束(Completed/Failed)。
 * */
sealed class TransferState {
    /**
     * 空闲状态，尚未开始传输。
     * */
    object Idle : TransferState()

    /**
     * 设备发现阶段。
     * */
    object Discovering : TransferState()

    /**
     * 设备连接阶段。
     *
     * @param device 正在连接的目标设备名称
     * */
    data class Connecting(val device: String) : TransferState()

    /**
     * 数据传输阶段。
     *
     * @param currentFileName 当前正在传输的文件名
     * @param progressPercent 当前文件传输进度百分比(0~100)
     * */
    data class Transferring(val currentFileName: String, val progressPercent: Float) : TransferState()

    /**
     * 传输结束且全部成功。
     * */
    object Completed : TransferState()

    /**
     * 传输结束且失败。
     *
     * @param message 失败原因描述
     * */
    data class Failed(val message: String) : TransferState()
}

/**
 * 单个文件的传输进度。
 *
 * @param relativePath 待推送文件的相对路径（以目标目录为根），例如 "a.txt" 或 "folder/sub/a.png"
 * @param bytesTransferred 已传输的字节数
 * @param totalBytes 文件总字节数
 * @param progressPercent 传输进度百分比(0~100)
 * */
data class TransferProgress(
    val relativePath: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val progressPercent: Float
)
