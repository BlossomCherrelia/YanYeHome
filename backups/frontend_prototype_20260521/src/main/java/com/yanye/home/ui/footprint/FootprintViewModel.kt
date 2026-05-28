package com.yanye.home.ui.footprint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yanye.home.data.local.database.YanYeDatabaseProvider
import com.yanye.home.data.sync.CloudBaseFootprintSyncService
import com.yanye.home.data.sync.DebouncedSyncController
import com.yanye.home.domain.model.CityLight
import com.yanye.home.domain.model.CityMemory
import com.yanye.home.domain.model.ProvinceLight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FootprintViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YanYeDatabaseProvider.footprintRepository(application)
    private val syncService = CloudBaseFootprintSyncService(application, repository)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    private val syncController = DebouncedSyncController(
        scope = viewModelScope,
        messageSink = _syncMessage,
        pendingMessage = "已保存，等待自动同步地图...",
        syncingMessage = "正在同步地图...",
        syncOperation = syncService::syncOnce,
        successMessage = { result ->
            result.skippedReason
                ?: "同步完成：上传 ${result.uploaded} 条，拉取 ${result.downloaded} 条"
        }
    )

    val provinceLights: StateFlow<List<ProvinceLight>> =
        repository.observeProvinceLights()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val cityMemories: StateFlow<List<CityMemory>> =
        repository.observeCityMemories()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val cityLights: StateFlow<List<CityLight>> =
        repository.observeCityLights()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun lightProvince(
        provinceName: String,
        fillColorArgb: Int = -34150
    ) {
        viewModelScope.launch {
            repository.saveProvinceLight(
                ProvinceLight(
                    provinceName = provinceName,
                    isLit = true,
                    fillColorArgb = fillColorArgb
                )
            )
            syncController.requestSync()
        }
    }

    fun saveProvinceLight(provinceLight: ProvinceLight) {
        viewModelScope.launch {
            repository.saveProvinceLight(provinceLight)
            syncController.requestSync()
        }
    }

    fun saveCityMemory(cityMemory: CityMemory) {
        viewModelScope.launch {
            repository.saveCityMemory(cityMemory)
            syncController.requestSync()
        }
    }

    fun saveCityLight(cityLight: CityLight) {
        viewModelScope.launch {
            repository.saveCityLight(cityLight)
            syncController.requestSync()
        }
    }

    fun deleteCityMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteCityMemory(id)
            syncController.requestSync()
        }
    }

    fun syncFootprints() {
        syncController.syncNow()
    }

    fun flushSync() {
        syncController.flushSync()
    }
}
