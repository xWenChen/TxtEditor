package com.wellcherish.texteditor.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * UI相关的异步计算动作的单线程线程池。
 */
object UIAsyncDispatcher : CoroutineDispatcher() {
    // 1. 定义一个线程池：1个核心线程，1个最大线程
    // 2. 队列长度设为 9 (等待任务)
    // 3. 拒绝策略设为 DiscardPolicy (直接丢弃新来的请求)
    private val threadPool = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(Int.MAX_VALUE),
        Executors.defaultThreadFactory(),
        /**
         * 核心：队列满时丢弃最新任务
         * */
        ThreadPoolExecutor.DiscardPolicy()
    )

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // 通过线程池分发任务
        // 如果线程池和队列都满了，DiscardPolicy 会生效
        threadPool.execute(block)
    }
}

/**
 * UI相关的异步计算动作的单线程线程池。
 */
// 语法糖：Dispatchers.uiAsync
val Dispatchers.uiAsync: CoroutineDispatcher
    get() = UIAsyncDispatcher