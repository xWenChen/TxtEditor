package com.wellcherish.datasync.wifi.transfer

import com.wellcherish.datasync.bean.TransferProgress
import org.json.JSONObject
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Socket 数据传输接收端。
 *
 * 在 [port] 上监听，接收来自 [P2pDataSender] 的文件流，保存到 [targetDir] 下。
 * */
internal class P2pDataReceiver(
    private val targetDir: File,
    private val port: Int = P2pDataSender.DEFAULT_PORT
) {

    /**
     * 监听并接收文件，返回进度流。
     */
    fun receive(): Flow<TransferProgress> = flow {
        val serverSocket = ServerSocket(port)
        try {
            // 等待发送端连接
            val socket = serverSocket.accept()
            try {
                val input = DataInputStream(socket.getInputStream())
                val buffer = ByteArray(64 * 1024)

                // 循环接收，直到对端关闭连接(input 返回 -1)
                while (true) {
                    val metaLength: Int
                    try {
                        metaLength = input.readInt()
                    } catch (e: java.io.EOFException) {
                        break // 正常结束
                    }

                    // 读取元数据 JSON
                    val metaBytes = ByteArray(metaLength)
                    input.readFully(metaBytes)
                    val meta = JSONObject(String(metaBytes, Charsets.UTF_8))
                    val relativePath = meta.getString(P2pDataSender.KEY_RELATIVE_PATH)
                    val totalFileSize = meta.getLong(P2pDataSender.KEY_TOTAL_FILE_SIZE)

                    // 写入目标路径
                    val target = File(targetDir, relativePath)
                    target.parentFile?.mkdirs()

                    // 读取 payload 并落盘
                    var received: Long = 0
                    FileOutputStream(target).use { fos ->
                        while (received < totalFileSize) {
                            val read = input.read(
                                buffer, 0, minOf(buffer.size.toLong(), totalFileSize - received).toInt()
                            )
                            if (read == -1) {
                                throw java.io.IOException("接收中断：文件数据不完整")
                            }
                            fos.write(buffer, 0, read)
                            received += read
                            emit(
                                TransferProgress(
                                    relativePath = relativePath,
                                    bytesTransferred = received,
                                    totalBytes = totalFileSize,
                                    progressPercent = if (totalFileSize > 0) {
                                        (received.toFloat() / totalFileSize * 100).coerceIn(0f, 100f)
                                    } else 100f
                                )
                            )
                        }
                    }
                }
            } finally {
                runCatching { socket.close() }
            }
        } finally {
            runCatching { serverSocket.close() }
        }
    }.flowOn(Dispatchers.IO)
}
