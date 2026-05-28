package com.yanye.home.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memos",
    indices = [
        Index(value = ["category"]),
        Index(value = ["visibility"]),
        Index(value = ["reminderAtMillis"]),
        Index(value = ["isCompleted"]),
        Index(value = ["linkedScheduleId"])
    ]
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val category: String = "GENERAL",
    val checklistItems: String = "",
    val imageUris: String = "",
    val reminderAtMillis: Long? = null,
    val reminderEnabled: Boolean = false,
    val notificationChannelKey: String = "memo_reminder",
    val linkedScheduleId: Long? = null,
    val visibility: String = "SHARED",
    val sharedWithPartner: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
