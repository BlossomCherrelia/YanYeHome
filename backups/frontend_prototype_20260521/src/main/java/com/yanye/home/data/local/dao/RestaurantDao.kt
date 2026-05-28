package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.RestaurantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {
    @Query(
        """
        SELECT * FROM restaurants
        WHERE isDeleted = 0
        ORDER BY isPitfall ASC, COALESCE(lastPickedAt, 0) ASC, createdAt DESC
        """
    )
    fun observeRestaurants(): Flow<List<RestaurantEntity>>

    @Query(
        """
        SELECT * FROM restaurants
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncRestaurants(): List<RestaurantEntity>

    @Query(
        """
        SELECT * FROM restaurants
        WHERE remoteId = :remoteId
        LIMIT 1
        """
    )
    suspend fun findByRemoteId(remoteId: String): RestaurantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurant(restaurant: RestaurantEntity): Long

    @Update
    suspend fun updateRestaurant(restaurant: RestaurantEntity)

    @Query(
        """
        UPDATE restaurants
        SET lastPickedAt = :pickedAt, updatedAt = :pickedAt
        WHERE id = :id
        """
    )
    suspend fun markPicked(id: Long, pickedAt: Long)

    @Query(
        """
        UPDATE restaurants
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteRestaurant(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE restaurants
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markSynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )
}
