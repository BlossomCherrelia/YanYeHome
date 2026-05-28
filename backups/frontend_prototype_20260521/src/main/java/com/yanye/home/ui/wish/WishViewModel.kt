package com.yanye.home.ui.wish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseWishSyncService
import com.yanye.home.data.sync.DebouncedSyncController
import com.yanye.home.domain.model.Wish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WishViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YanYeDatabaseProvider.wishRepository(application)
    private val syncService = CloudBaseWishSyncService(application, repository)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val syncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步愿望...",
        syncingMessage = "正在同步愿望...",
        syncOperation = syncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )

    val wishes: StateFlow<List<Wish>> =
        repository.observeWishes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun saveWish(wish: Wish) {
        viewModelScope.launch {
            repository.saveWish(wish)
            syncController.requestSync()
        }
    }

    fun setWishCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setWishCompleted(id, isCompleted)
            syncController.requestSync()
        }
    }

    fun deleteWish(id: Long) {
        viewModelScope.launch {
            repository.deleteWish(id)
            syncController.requestSync()
        }
    }

    fun syncWishes() {
        syncController.syncNow()
    }

    fun flushSync() {
        syncController.flushSync()
    }
}
