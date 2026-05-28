package com.yanye.home.domain.model

data class Anniversary(
    val id: Long = 0,
    val name: String,
    val dateEpochDay: Long,
    val type: AnniversaryType = AnniversaryType.Custom,
    val displayMode: AnniversaryDisplayMode = AnniversaryDisplayMode.Anniversary,
    val reminderDaysBefore: Int = 7,
    val note: String = "",
    val coverImageUri: String? = null,
    val giftWishLinkEnabled: Boolean = true,
    val celebrationArchiveEnabled: Boolean = true,
    val createdBy: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val visibility: Visibility = Visibility.Shared,
    val sharedWithPartner: Boolean = true,
    val lockedUntilEpochDay: Long? = null,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null,
    val showOnHome: Boolean = false,
    val homeSortOrder: Int = 100
)

enum class SyncStatus(val storageValue: String) {
    Synced("SYNCED"),
    PendingCreate("PENDING_CREATE"),
    PendingUpdate("PENDING_UPDATE"),
    PendingDelete("PENDING_DELETE");

    companion object {
        fun fromStorageValue(value: String): SyncStatus =
            entries.firstOrNull { it.storageValue == value } ?: Synced
    }
}

enum class AnniversaryType(val storageValue: String) {
    Relationship("RELATIONSHIP"),
    Birthday("BIRTHDAY"),
    Travel("TRAVEL"),
    Custom("CUSTOM");

    companion object {
        fun fromStorageValue(value: String): AnniversaryType =
            entries.firstOrNull { it.storageValue == value } ?: Custom
    }
}

enum class AnniversaryDisplayMode(val storageValue: String) {
    Countdown("COUNTDOWN"),
    CountUp("COUNT_UP"),
    Anniversary("ANNIVERSARY");

    companion object {
        fun fromStorageValue(value: String): AnniversaryDisplayMode =
            entries.firstOrNull { it.storageValue == value } ?: Anniversary
    }
}

enum class Visibility(val storageValue: String) {
    Private("PRIVATE"),
    Shared("SHARED"),
    Partial("PARTIAL"),
    RevealAfterDate("REVEAL_AFTER_DATE");

    companion object {
        fun fromStorageValue(value: String): Visibility =
            entries.firstOrNull { it.storageValue == value } ?: Shared
    }
}
