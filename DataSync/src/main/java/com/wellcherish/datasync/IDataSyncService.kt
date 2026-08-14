package com.wellcherish.datasync

import com.wellcherish.datasync.bean.TransferProgress
import com.wellcherish.datasync.bean.TransferState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 数据同步服务的抽象接口。UI 层仅依赖此接口，不感知具体实现方式（例如 WiFi P2P）。
 * */
interface IDataSyncService {

    /**
     * 传输生命周期状态流：
     * Idle → Discovering(设备发现) → Connecting(设备连接) → Transferring(数据传输) → Completed/Failed(传输结束)。
     * */
    val stateFlow: StateFlow<TransferState>

    /**
     * 发送一个或多个文件/文件夹。
     *
     * [targets] 可混合包含普通文件与文件夹。若包含文件夹，则递归传输其中所有文件。
     *
     * @return 传输进度流
     * */
    fun sendFiles(targets: List<File>): Flow<TransferProgress>

    /**
     * 接收文件到指定目标目录。
     *
     * @param targetDir 接收文件保存的根目录（调用方传入，如 app 的默认文本存储目录）
     * @return 传输进度流
     * */
    fun receive(targetDir: File): Flow<TransferProgress>
}
