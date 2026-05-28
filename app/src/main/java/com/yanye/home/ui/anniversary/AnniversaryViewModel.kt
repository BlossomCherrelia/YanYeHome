package com.yanye.home.ui.anniversary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseAnniversarySyncService
import com.yanye.home.data.sync.DebouncedSyncController
import com.yanye.home.domain.model.Anniversary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YanYeDatabaseProvider.anniversaryRepository(application)
    private val syncService = CloudBaseAnniversarySyncService(application, repository)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val syncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步纪念日...",
        syncingMessage = "正在同步纪念日...",
        syncOperation = syncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )

    val anniversaries: StateFlow<List<Anniversary>> =
        repository.observeAnniversaries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun saveAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.saveAnniversary(anniversary)
            syncController.requestSync()
        }
    }

    fun deleteAnniversary(id: Long) {
        viewModelScope.launch {
            repository.deleteAnniversary(id)
            syncController.requestSync()
        }
    }

    fun syncAnniversaries() {
        syncController.syncNow()
    }

    fun flushSync() {
        syncController.flushSync()
    }
}
