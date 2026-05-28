package com.yanye.home.domain.model

data class ProvinceLight(
    val id: Long = 0,
    val provinceName: String,
    val isLit: Boolean = true,
    val fillColorArgb: Int = -34150,
    val note: String = "",
    val linkedScheduleId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

data class CityMemory(
    val id: Long = 0,
    val provinceName: String,
    val cityName: String,
    val title: String = "",
    val dateEpochDay: Long,
    val foods: String = "",
    val places: String = "",
    val photoUris: String = "",
    val linkedScheduleId: Long? = null,
    val insideJoke: String = "",
    val expenseCents: Int? = null,
    val pitfallNotes: String = "",
    val rating: Int = 0,
    val note: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

data class CityLight(
    val id: Long = 0,
    val provinceName: String,
    val cityName: String,
    val isLit: Boolean = true,
    val fillColorArgb: Int = -34150,
    val note: String = "",
    val linkedScheduleId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)
