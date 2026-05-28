package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    indices = [
        Index(value = ["startEpochDay"]),
        Index(value = ["linkedWishId"]),
        Index(value = ["isCompleted"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"]),
        Index(value = ["linkedWishRemoteId"])
    ]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startEpochDay: Long,
    val startMinuteOfDay: Int = 19 * 60,
    val endMinuteOfDay: Int? = null,
    val locationName: String = "",
    val reminderMinutesBefore: Int = 60,
    val budgetCents: Long? = null,
    val participants: String = "我们俩",
    val linkedWishId: Long? = null,
    val linkedWishRemoteId: String? = null,
    val isGuideMode: Boolean = false,
    val guideRestaurants: String = "",
    val guideActivities: String = "",
    val guideRoute: String = "",
    val backupPlan: String = "",
    val note: String = "",
    val isCompleted: Boolean = false,
    val memoryId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val visibility: String = "SHARED",
    val sharedWithPartner: Boolean = true,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null
)
