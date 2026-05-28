package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "anniversaries",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["visibility"]),
        Index(value = ["remoteId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["coupleId"])
    ]
)
data class AnniversaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateEpochDay: Long,
    val type: String = "CUSTOM",
    val displayMode: String = "ANNIVERSARY",
    val reminderDaysBefore: Int = 7,
    val note: String = "",
    val coverImageUri: String? = null,
    val giftWishLinkEnabled: Boolean = true,
    val celebrationArchiveEnabled: Boolean = true,
    val createdBy: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val visibility: String = "SHARED",
    val sharedWithPartner: Boolean = true,
    val lockedUntilEpochDay: Long? = null,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: String = "SYNCED",
    val remoteUpdatedAt: Long? = null,
    val showOnHome: Boolean = false,
    val homeSortOrder: Int = 100
)
