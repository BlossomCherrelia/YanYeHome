package com.yanye.home.domain.model

data class Schedule(
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
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val visibility: Visibility = Visibility.Shared,
    val sharedWithPartner: Boolean = true,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

data class Memory(
    val id: Long = 0,
    val title: String,
    val dateEpochDay: Long,
    val scheduleId: Long? = null,
    val linkedWishId: Long? = null,
    val locationName: String = "",
    val photoUris: String = "",
    val foodNotes: String = "",
    val expenseCents: Long? = null,
    val mood: MemoryMood = MemoryMood.Happy,
    val note: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

enum class MemoryMood(val storageValue: String) {
    Happy("HAPPY"),
    Touched("TOUCHED"),
    TiredButWorth("TIRED_BUT_WORTH"),
    Ordinary("ORDINARY"),
    Unwell("UNWELL");

    companion object {
        fun fromStorageValue(value: String): MemoryMood =
            entries.firstOrNull { it.storageValue == value } ?: Happy
    }
}
