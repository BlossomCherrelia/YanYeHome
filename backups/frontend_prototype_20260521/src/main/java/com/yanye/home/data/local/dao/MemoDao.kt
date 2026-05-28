package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.CareCycleEntity
import com.yanye.home.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query(
        """
        SELECT * FROM memos
        WHERE isDeleted = 0
        ORDER BY isCompleted ASC,
            CASE WHEN reminderAtMillis IS NULL THEN 1 ELSE 0 END ASC,
            reminderAtMillis ASC,
            updatedAt DESC
        """
    )
    fun observeMemos(): Flow<List<MemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity): Long

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Query(
        """
        UPDATE memos
        SET isCompleted = :isCompleted, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setMemoCompleted(id: Long, isCompleted: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE memos
        SET isDeleted = 1, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteMemo(id: Long, updatedAt: Long)

    @Query(
        """
        SELECT * FROM care_cycles
        WHERE isDeleted = 0
        ORDER BY startEpochDay DESC, createdAt DESC
        """
    )
    fun observeCareCycles(): Flow<List<CareCycleEntity>>

    @Query(
        """
        SELECT * FROM care_cycles
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCareCycles(): List<CareCycleEntity>

    @Query("SELECT * FROM care_cycles WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findCareCycleByRemoteId(remoteId: String): CareCycleEntity?

    @Query("SELECT * FROM care_cycles WHERE startEpochDay = :startEpochDay LIMIT 1")
    suspend fun findCareCycleByStart(startEpochDay: Long): CareCycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareCycle(careCycle: CareCycleEntity): Long

    @Query(
        """
        UPDATE care_cycles
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteCareCycle(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE care_cycles
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE isDeleted = 0
        """
    )
    suspend fun softDeleteAllCareCycles(updatedAt: Long)

    @Query(
        """
        UPDATE care_cycles
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markCareCycleSynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )
}
