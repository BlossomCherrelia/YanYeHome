package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "care_cycles",
    indices = [
        Index(value = ["startEpochDay"]),
        Index(value = ["shareReminderWithPartner"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"])
    ]
)
data class CareCycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val cycleLengthDays: Int = 28,
    val painLevel: String = "NONE",
    val mood: String = "STABLE",
    val avoidNotes: String = "",
    val carePreference: String = "",
    val shareReminderWithPartner: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null
)
