package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.WishEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishDao {
    @Query(
        """
        SELECT * FROM wishes
        WHERE isDeleted = 0
        ORDER BY isCompleted ASC,
            CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
            COALESCE(targetDateEpochDay, 9223372036854775807) ASC,
            createdAt DESC
        """
    )
    fun observeWishes(): Flow<List<WishEntity>>

    @Query(
        """
        SELECT * FROM wishes
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY isCompleted ASC,
            CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
            COALESCE(targetDateEpochDay, 9223372036854775807) ASC,
            createdAt DESC
        """
    )
    fun observeWishesForCouple(coupleId: String): Flow<List<WishEntity>>

    @Query(
        """
        SELECT * FROM wishes
        WHERE id = :id AND isDeleted = 0
        LIMIT 1
        """
    )
    fun observeWish(id: Long): Flow<WishEntity?>

    @Query(
        """
        SELECT * FROM wishes
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun findById(id: Long): WishEntity?

    @Query(
        """
        SELECT * FROM wishes
        WHERE syncStatus != 'SYNCED'
           OR (remoteId IS NULL AND visibility != 'PRIVATE')
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncWishes(): List<WishEntity>

    @Query(
        """
        SELECT * FROM wishes
        WHERE (syncStatus != 'SYNCED'
           OR (remoteId IS NULL AND visibility != 'PRIVATE'))
          AND coupleId = :coupleId
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncWishesForCouple(coupleId: String): List<WishEntity>

    @Query(
        """
        SELECT * FROM wishes
        WHERE remoteId = :remoteId
        LIMIT 1
        """
    )
    suspend fun findByRemoteId(remoteId: String): WishEntity?

    @Query(
        """
        SELECT * FROM wishes
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findByRemoteIdForCouple(remoteId: String, coupleId: String): WishEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWish(wish: WishEntity): Long

    @Update
    suspend fun updateWish(wish: WishEntity)

    @Query(
        """
        UPDATE wishes
        SET isCompleted = :isCompleted,
            syncStatus = CASE WHEN visibility = 'PRIVATE' THEN syncStatus ELSE 'PENDING_UPDATE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setWishCompleted(id: Long, isCompleted: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE wishes
        SET linkedScheduleId = :scheduleId,
            syncStatus = CASE WHEN visibility = 'PRIVATE' THEN syncStatus ELSE 'PENDING_UPDATE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setLinkedSchedule(id: Long, scheduleId: Long?, updatedAt: Long)

    @Query(
        """
        UPDATE wishes
        SET isDeleted = 1,
            syncStatus = CASE WHEN visibility = 'PRIVATE' AND remoteId IS NULL THEN syncStatus ELSE 'PENDING_DELETE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteWish(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE wishes
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

    @Query(
        """
        UPDATE wishes
        SET remoteId = NULL,
            coupleId = NULL,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markPrivateRevokeSynced(
        id: Long,
        ownerUserId: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE wishes
        SET linkedScheduleId = NULL,
            syncStatus = CASE WHEN visibility = 'PRIVATE' THEN syncStatus ELSE 'PENDING_UPDATE' END,
            updatedAt = :updatedAt
        WHERE linkedScheduleId IS NOT NULL
        """
    )
    suspend fun clearLinkedSchedules(updatedAt: Long)

}
