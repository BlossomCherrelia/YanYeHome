package com.yanye.home.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DebouncedSyncController<T>(
    private val scope: CoroutineScope,
    private val messageSink: MutableStateFlow<String?>,
    private val pendingMessage: String,
    private val syncingMessage: String,
    private val successMessage: (T) -> String,
    private val syncOperation: suspend () -> Result<T>,
    private val delayMillis: Long = DEFAULT_DELAY_MILLIS
) {
    private val mutex = Mutex()
    private var scheduledJob: Job? = null

    fun requestSync() {
        scheduledJob?.cancel()
        messageSink.value = pendingMessage
        scheduledJob = scope.launch {
            delay(delayMillis)
            scheduledJob = null
            performSync()
        }
    }

    fun syncNow() {
        scheduledJob?.cancel()
        scheduledJob = null
        scope.launch {
            performSync()
        }
    }

    fun flushSync() {
        syncNow()
    }

    private suspend fun performSync() {
        mutex.withLock {
            messageSink.value = syncingMessage
            val result = syncOperation()
            messageSink.value = result.fold(
                onSuccess = successMessage,
                onFailure = { error ->
                    "同步失败：${error.message ?: "未知错误"}"
                }
            )
        }
    }

    private companion object {
        const val DEFAULT_DELAY_MILLIS = 10_000L
    }
}
