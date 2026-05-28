package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurants",
    indices = [
        Index(value = ["cuisine"]),
        Index(value = ["cityName"]),
        Index(value = ["isPitfall"]),
        Index(value = ["lastPickedAt"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"])
    ]
)
data class RestaurantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cuisine: String = "",
    val tags: String = "",
    val avoidNotes: String = "",
    val priceLevel: String = "MEDIUM",
    val address: String = "",
    val cityName: String = "",
    val canTakeout: Boolean = true,
    val canDineIn: Boolean = true,
    val isPitfall: Boolean = false,
    val rating: Int = 0,
    val note: String = "",
    val lastPickedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null
)
