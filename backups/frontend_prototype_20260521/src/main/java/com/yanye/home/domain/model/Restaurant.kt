package com.yanye.home.domain.model

data class Restaurant(
    val id: Long = 0,
    val name: String,
    val cuisine: String = "",
    val tags: String = "",
    val avoidNotes: String = "",
    val priceLevel: RestaurantPriceLevel = RestaurantPriceLevel.Medium,
    val address: String = "",
    val cityName: String = "",
    val canTakeout: Boolean = true,
    val canDineIn: Boolean = true,
    val isPitfall: Boolean = false,
    val rating: Int = 0,
    val note: String = "",
    val lastPickedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
    val remoteId: String? = null,
    val coupleId: String? = null,
    val ownerUserId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val remoteUpdatedAt: Long? = null
)

enum class RestaurantPriceLevel(val storageValue: String) {
    Low("LOW"),
    Medium("MEDIUM"),
    High("HIGH");

    companion object {
        fun fromStorageValue(value: String): RestaurantPriceLevel =
            entries.firstOrNull { it.storageValue == value } ?: Medium
    }
}
