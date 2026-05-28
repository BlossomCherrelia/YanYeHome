package com.yanye.home.ui.care

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseCareCycleSyncService
import com.yanye.home.data.sync.DebouncedSyncController
import com.yanye.home.domain.model.CareCycle
import com.yanye.home.domain.model.Memo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CareViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YanYeDatabaseProvider.memoRepository(application)
    private val syncService = CloudBaseCareCycleSyncService(application, repository)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val syncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步经期记录...",
        syncingMessage = "正在同步经期记录...",
        syncOperation = syncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )

    val memos: StateFlow<List<Memo>> =
        repository.observeMemos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val careCycles: StateFlow<List<CareCycle>> =
        repository.observeCareCycles()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun saveMemo(memo: Memo) {
        viewModelScope.launch {
            repository.saveMemo(memo)
        }
    }

    fun setMemoCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setMemoCompleted(id, isCompleted)
        }
    }

    fun deleteMemo(id: Long) {
        viewModelScope.launch {
            repository.deleteMemo(id)
        }
    }

    fun saveCareCycle(careCycle: CareCycle) {
        viewModelScope.launch {
            repository.saveCareCycle(careCycle)
            syncController.requestSync()
        }
    }

    fun deleteCareCycle(id: Long) {
        viewModelScope.launch {
            repository.deleteCareCycle(id)
            syncController.requestSync()
        }
    }

    fun clearCareCycles() {
        viewModelScope.launch {
            repository.clearCareCycles()
            syncController.requestSync()
        }
    }

    fun syncCareCycles() {
        syncController.syncNow()
    }

    fun flushSync() {
        syncController.flushSync()
    }
}
