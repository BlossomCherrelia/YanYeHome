package com.yanye.home.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseAnniversarySyncService
import com.yanye.home.data.sync.CloudBaseMemorySyncService
import com.yanye.home.data.sync.CloudBaseScheduleSyncService
import com.yanye.home.data.sync.CloudBaseWishSyncService
import com.yanye.home.data.sync.DebouncedSyncController
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.Schedule
import com.yanye.home.domain.model.Wish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val scheduleRepository = YanYeDatabaseProvider.scheduleRepository(application)
    private val wishRepository = YanYeDatabaseProvider.wishRepository(application)
    private val anniversaryRepository = YanYeDatabaseProvider.anniversaryRepository(application)
    private val syncService = CloudBaseScheduleSyncService(application, scheduleRepository)
    private val memorySyncService = CloudBaseMemorySyncService(application, scheduleRepository)
    private val wishSyncService = CloudBaseWishSyncService(application, wishRepository)
    private val anniversarySyncService = CloudBaseAnniversarySyncService(application, anniversaryRepository)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val relatedWishSyncMessage = MutableStateFlow<String?>(null)
    private val syncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步日程...",
        syncingMessage = "正在同步日程...",
        syncOperation = syncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )
    private val memorySyncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步回忆...",
        syncingMessage = "正在同步回忆...",
        syncOperation = memorySyncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "回忆同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )
    private val relatedWishSyncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = relatedWishSyncMessage,
        pendingMessage = "愿望联动待同步",
        syncingMessage = "正在同步愿望联动...",
        syncOperation = wishSyncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "愿望联动同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )
    private val anniversarySyncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步纪念日...",
        syncingMessage = "正在同步纪念日...",
        syncOperation = anniversarySyncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "纪念日同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )

    init {
        viewModelScope.launch {
            anniversaryRepository.ensureDefaultRelationshipAnniversary()
        }
    }

    val schedules: StateFlow<List<Schedule>> =
        scheduleRepository.observeSchedules()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val wishes: StateFlow<List<Wish>> =
        wishRepository.observeWishes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val memories: StateFlow<List<Memory>> =
        scheduleRepository.observeMemories()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val anniversaries: StateFlow<List<Anniversary>> =
        anniversaryRepository.observeAnniversaries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.saveSchedule(schedule)
            syncController.requestSync()
            relatedWishSyncController.requestSync()
        }
    }

    fun completeAndArchive(schedule: Schedule, memory: Memory) {
        viewModelScope.launch {
            scheduleRepository.completeAndArchive(schedule, memory)
            syncController.requestSync()
            memorySyncController.requestSync()
            relatedWishSyncController.requestSync()
        }
    }

    fun saveMemory(memory: Memory) {
        viewModelScope.launch {
            scheduleRepository.saveMemory(memory)
            memorySyncController.requestSync()
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(id)
            syncController.requestSync()
            relatedWishSyncController.requestSync()
        }
    }

    fun syncSchedules() {
        syncController.syncNow()
    }

    fun syncMemories() {
        memorySyncController.syncNow()
    }

    fun syncAnniversaries() {
        anniversarySyncController.syncNow()
    }

    fun saveAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            anniversaryRepository.saveAnniversary(anniversary)
            anniversarySyncController.requestSync()
        }
    }

    fun saveAnniversaryHomeDisplaySettings(anniversaries: List<Anniversary>) {
        viewModelScope.launch {
            anniversaries.forEach { anniversary ->
                anniversaryRepository.saveAnniversary(anniversary)
            }
            anniversarySyncController.requestSync()
        }
    }

    fun deleteAnniversary(id: Long) {
        viewModelScope.launch {
            anniversaryRepository.deleteAnniversary(id)
            anniversarySyncController.requestSync()
        }
    }

    fun clearSchedulesAndMemories() {
        viewModelScope.launch {
            scheduleRepository.clearSchedulesAndMemories()
            _syncMessage.value = "已清空本机日程和回忆卡"
            relatedWishSyncController.requestSync()
        }
    }

    fun flushSync() {
        syncController.flushSync()
        memorySyncController.flushSync()
        relatedWishSyncController.flushSync()
        anniversarySyncController.flushSync()
    }
}
