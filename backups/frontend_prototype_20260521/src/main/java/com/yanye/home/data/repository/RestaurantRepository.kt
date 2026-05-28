package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.RestaurantDao
import com.yanye.home.data.local.entity.RestaurantEntity
import com.yanye.home.domain.model.Restaurant
import com.yanye.home.domain.model.RestaurantPriceLevel
import com.yanye.home.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RestaurantRepository(
    private val restaurantDao: RestaurantDao
) {
    fun observeRestaurants(): Flow<List<Restaurant>> =
        restaurantDao.observeRestaurants()
            .map { restaurants -> restaurants.map(RestaurantEntity::toDomain) }

    suspend fun saveRestaurant(restaurant: Restaurant): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (restaurant.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val entity = restaurant.toEntity(
            createdAt = restaurant.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
        )

        return if (restaurant.id == 0L) {
            restaurantDao.insertRestaurant(entity)
        } else {
            restaurantDao.updateRestaurant(entity)
            restaurant.id
        }
    }

    suspend fun markPicked(id: Long) {
        restaurantDao.markPicked(
            id = id,
            pickedAt = System.currentTimeMillis()
        )
    }

    suspend fun pendingSyncRestaurants(): List<Restaurant> =
        restaurantDao.pendingSyncRestaurants()
            .map(RestaurantEntity::toDomain)

    suspend fun saveRemoteRestaurant(restaurant: Restaurant): Long {
        val now = System.currentTimeMillis()
        val entity = restaurant.toEntity(
            createdAt = restaurant.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = restaurant.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val local = restaurant.remoteId?.let { restaurantDao.findByRemoteId(it) }
        if (local == null && restaurant.isDeleted) {
            return 0L
        }

        return if (local == null) {
            restaurantDao.insertRestaurant(entity)
        } else {
            restaurantDao.updateRestaurant(entity.copy(id = local.id, lastPickedAt = local.lastPickedAt))
            local.id
        }
    }

    suspend fun markSynced(
        localId: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long
    ) {
        restaurantDao.markSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteRestaurant(id: Long) {
        restaurantDao.softDeleteRestaurant(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }
}

private fun RestaurantEntity.toDomain(): Restaurant =
    Restaurant(
        id = id,
        name = name,
        cuisine = cuisine,
        tags = tags,
        avoidNotes = avoidNotes,
        priceLevel = RestaurantPriceLevel.fromStorageValue(priceLevel),
        address = address,
        cityName = cityName,
        canTakeout = canTakeout,
        canDineIn = canDineIn,
        isPitfall = isPitfall,
        rating = rating,
        note = note,
        lastPickedAt = lastPickedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun Restaurant.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): RestaurantEntity =
    RestaurantEntity(
        id = id,
        name = name,
        cuisine = cuisine,
        tags = tags,
        avoidNotes = avoidNotes,
        priceLevel = priceLevel.storageValue,
        address = address,
        cityName = cityName,
        canTakeout = canTakeout,
        canDineIn = canDineIn,
        isPitfall = isPitfall,
        rating = rating,
        note = note,
        lastPickedAt = lastPickedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )
