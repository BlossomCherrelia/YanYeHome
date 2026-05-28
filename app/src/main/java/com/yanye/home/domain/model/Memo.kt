package com.yanye.home.domain.model

data class Memo(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val category: MemoCategory = MemoCategory.General,
    val checklistItems: String = "",
    val imageUris: String = "",
    val dueLabel: String = "",
    val reminderAtMillis: Long? = null,
    val reminderEnabled: Boolean = false,
    val notificationChannelKey: String = "memo_reminder",
    val linkedScheduleId: Long? = null,
    val visibility: Visibility = Visibility.Shared,
    val sharedWithPartner: Boolean = true,
    val isCompleted: Boolean = false,
    val showOnHome: Boolean = false,
    val homeSortOrder: Int = 100,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

data class CareCycle(
    val id: Long = 0,
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val cycleLengthDays: Int = 28,
    val painLevel: CarePainLevel = CarePainLevel.None,
    val mood: CareMood = CareMood.Stable,
    val avoidNotes: String = "",
    val carePreference: String = "",
    val shareReminderWithPartner: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

enum class MemoCategory(val storageValue: String) {
    General("GENERAL"),
    Bring("BRING"),
    Medicine("MEDICINE"),
    Errand("ERRAND"),
    Commute("COMMUTE"),
    Care("CARE");

    companion object {
        fun fromStorageValue(value: String): MemoCategory =
            entries.firstOrNull { it.storageValue == value } ?: General
    }
}

enum class CarePainLevel(val storageValue: String) {
    None("NONE"),
    Mild("MILD"),
    Medium("MEDIUM"),
    Strong("STRONG");

    companion object {
        fun fromStorageValue(value: String): CarePainLevel =
            entries.firstOrNull { it.storageValue == value } ?: None
    }
}

enum class CareMood(val storageValue: String) {
    Stable("STABLE"),
    Tired("TIRED"),
    Sensitive("SENSITIVE"),
    Low("LOW");

    companion object {
        fun fromStorageValue(value: String): CareMood =
            entries.firstOrNull { it.storageValue == value } ?: Stable
    }
}
