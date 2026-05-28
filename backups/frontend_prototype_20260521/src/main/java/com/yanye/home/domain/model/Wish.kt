package com.yanye.home.domain.model

data class Wish(
    val id: Long = 0,
    val title: String,
    val category: WishCategory = WishCategory.Custom,
    val visibility: Visibility = Visibility.Shared,
    val revealAfterEpochDay: Long? = null,
    val budgetCents: Long? = null,
    val locationName: String = "",
    val priority: WishPriority = WishPriority.Medium,
    val targetDateEpochDay: Long? = null,
    val note: String = "",
    val preparationItems: String = "",
    val coverImageUri: String? = null,
    val isCompleted: Boolean = false,
    val scheduleReady: Boolean = true,
    val linkedScheduleId: Long? = null,
    val giftCandidateForAnniversaryId: Long? = null,
    val createdBy: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val sharedWithPartner: Boolean = true,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

enum class WishCategory(val storageValue: String) {
    Shopping("SHOPPING"),
    Travel("TRAVEL"),
    Restaurant("RESTAURANT"),
    Gift("GIFT"),
    Home("HOME"),
    Pet("PET"),
    Movie("MOVIE"),
    Game("GAME"),
    Custom("CUSTOM");

    companion object {
        fun fromStorageValue(value: String): WishCategory =
            entries.firstOrNull { it.storageValue == value } ?: Custom
    }
}

enum class WishPriority(val storageValue: String) {
    Low("LOW"),
    Medium("MEDIUM"),
    High("HIGH");

    companion object {
        fun fromStorageValue(value: String): WishPriority =
            entries.firstOrNull { it.storageValue == value } ?: Medium
    }
}
