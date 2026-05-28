package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["scheduleId"]),
        Index(value = ["linkedWishId"])
    ]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateEpochDay: Long,
    val scheduleId: Long? = null,
    val linkedWishId: Long? = null,
    val locationName: String = "",
    val photoUris: String = "",
    val foodNotes: String = "",
    val expenseCents: Long? = null,
    val mood: String = "HAPPY",
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
