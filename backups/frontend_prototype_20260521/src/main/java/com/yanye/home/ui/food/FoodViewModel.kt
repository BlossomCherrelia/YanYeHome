package com.yanye.home.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseRestaurantSyncService
import com.yanye.home.data.sync.DebouncedSyncController
import com.yanye.home.domain.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YanYeDatabaseProvider.restaurantRepository(application)
    private val syncService = CloudBaseRestaurantSyncService(application, repository)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val syncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步餐厅池...",
        syncingMessage = "正在同步餐厅池...",
        syncOperation = syncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )

    val restaurants: StateFlow<List<Restaurant>> =
        repository.observeRestaurants()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun saveRestaurant(restaurant: Restaurant) {
        viewModelScope.launch {
            repository.saveRestaurant(restaurant)
            syncController.requestSync()
        }
    }

    fun markPicked(id: Long) {
        viewModelScope.launch {
            repository.markPicked(id)
        }
    }

    fun deleteRestaurant(id: Long) {
        viewModelScope.launch {
            repository.deleteRestaurant(id)
            syncController.requestSync()
        }
    }

    fun syncRestaurants() {
        syncController.syncNow()
    }

    fun flushSync() {
        syncController.flushSync()
    }
}
