package com.yanye.home.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseMemoSyncService
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.Memo
import com.yanye.home.domain.model.Schedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val anniversaryRepository = YanYeDatabaseProvider.anniversaryRepository(application)
    private val scheduleRepository = YanYeDatabaseProvider.scheduleRepository(application)
    private val memoRepository = YanYeDatabaseProvider.memoRepository(application)
    private val memoSyncService = CloudBaseMemoSyncService(application, memoRepository)

    val anniversaries: StateFlow<List<Anniversary>> =
        anniversaryRepository.observeAnniversaries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val schedules: StateFlow<List<Schedule>> =
        scheduleRepository.observeSchedules()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val memos: StateFlow<List<Memo>> =
        memoRepository.observeMemos()
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

    fun setMemoCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            memoRepository.setMemoCompleted(id, isCompleted)
            memoSyncService.syncOnce()
        }
    }

    init {
        viewModelScope.launch {
            anniversaryRepository.ensureDefaultRelationshipAnniversary()
        }
    }
}
