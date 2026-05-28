package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wishes",
    indices = [
        Index(value = ["category"]),
        Index(value = ["visibility"]),
        Index(value = ["targetDateEpochDay"]),
        Index(value = ["isCompleted"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"])
    ]
)
data class WishEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "CUSTOM",
    val visibility: String = "SHARED",
    val revealAfterEpochDay: Long? = null,
    val budgetCents: Long? = null,
    val locationName: String = "",
    val priority: String = "MEDIUM",
    val targetDateEpochDay: Long? = null,
    val note: String = "",
    val preparationItems: String = "",
    val coverImageUri: String? = null,
    val isCompleted: Boolean = false,
    val scheduleReady: Boolean = true,
    val linkedScheduleId: Long? = null,
    val giftCandidateForAnniversaryId: Long? = null,
    val createdBy: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sharedWithPartner: Boolean = true,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null
)
