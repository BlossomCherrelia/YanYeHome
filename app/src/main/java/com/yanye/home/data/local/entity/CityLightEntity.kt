package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "city_lights",
    indices = [
        Index(value = ["provinceName"]),
        Index(value = ["cityName"]),
        Index(value = ["isLit"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"]),
        Index(value = ["coupleId", "provinceName", "cityName"], unique = true)
    ]
)
data class CityLightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val provinceName: String,
    val cityName: String,
    val isLit: Boolean = true,
    val fillColorArgb: Int = -34150,
    val note: String = "",
    val linkedScheduleId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null
)
