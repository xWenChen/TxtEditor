package com.wellcherish.datasync.wifi.transfer

import com.wellcherish.datasync.bean.TransferProgress
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Socket 数据传输发送端。
 *
 * 传输协议：
 * 1. 每次发送一个文件前，先发送 4 字节(元数据长度) + JSON 元数据头；
 * 2. 再发送文件原始字节流。
 *
 * [targets] 可混合普通文件与文件夹，文件夹将递归展开为若干文件。
 * */
internal class P2pDataSender(
    private val targetIp: String,
    private val port: Int = DEFAULT_PORT
) {

    /**
     * 发送 [targets] 中全部文件，返回进度流。
     * */
    fun send(targets: List<File>): Flow<TransferProgress> = flow {
        // 1. 扁平化：解析出 (物理文件,相对路径) 列表
        val flattened = mutableListOf<Pair<File, String>>()
        targets.forEach { file ->
            if (file.isDirectory) {
                file.walkTopDown().filter { it.isFile }.forEach { sub ->
                    val relative = file.name + "/" + sub.relativeTo(file).path
                    flattened.add(sub to relative.replace("\\", "/"))
                }
            } else {
                flattened.add(file to file.name)
            }
        }

        // 2. 建立 socket 连接
        val socket = Socket(targetIp, port)
        try {
            socket.tcpNoDelay = true
            val output = DataOutputStream(socket.getOutputStream())
            val buffer = ByteArray(64 * 1024) // 64KB 缓冲区

            // 3. 逐个文件发送
            flattened.forEach { (file, relativePath) ->
                val meta = JSONObject().apply {
                    put(KEY_RELATIVE_PATH, relativePath)
                    put(KEY_TOTAL_FILE_SIZE, file.length())
                    put(KEY_IS_DIRECTORY, false)
                }
                val metaBytes = meta.toString().toByteArray(Charsets.UTF_8)

                // 写入 4 字节元数据长度
                output.writeInt(metaBytes.size)
                output.write(metaBytes)

                // 发送文件 payload，并计算进度
                val fileLength = file.length()
                var transferred: Long = 0
                FileInputStream(file).use { fis ->
                    var read = fis.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        transferred += read
                        emit(
                            TransferProgress(
                                relativePath = relativePath,
                                bytesTransferred = transferred,
                                totalBytes = fileLength,
                                progressPercent = if (fileLength > 0) {
                                    (transferred.toFloat() / fileLength * 100).coerceIn(0f, 100f)
                                } else 100f
                            )
                        )
                        read = fis.read(buffer)
                    }
                }
                output.flush()
            }
        } finally {
            runCatching { socket.close() }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val DEFAULT_PORT = 8899

        // 元数据字段名
        const val KEY_RELATIVE_PATH = "relativePath"
        const val KEY_TOTAL_FILE_SIZE = "totalFileSize"
        const val KEY_IS_DIRECTORY = "isDirectory"
    }
}
