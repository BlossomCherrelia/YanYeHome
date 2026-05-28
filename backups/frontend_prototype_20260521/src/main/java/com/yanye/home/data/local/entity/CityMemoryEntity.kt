package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "city_memories",
    indices = [
        Index(value = ["provinceName"]),
        Index(value = ["cityName"]),
        Index(value = ["dateEpochDay"]),
        Index(value = ["linkedScheduleId"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"])
    ]
)
data class CityMemoryEntity(
    @PrimaryKey(autoGenerate = true)
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
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null
)
