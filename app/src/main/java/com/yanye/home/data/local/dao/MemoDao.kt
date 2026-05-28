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

    @Query(
        """
        SELECT * FROM memos
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY isCompleted ASC,
            CASE WHEN reminderAtMillis IS NULL THEN 1 ELSE 0 END ASC,
            reminderAtMillis ASC,
            updatedAt DESC
        """
    )
    fun observeMemosForCouple(coupleId: String): Flow<List<MemoEntity>>

    @Query(
        """
        SELECT * FROM memos
        WHERE syncStatus != 'SYNCED'
           OR (remoteId IS NULL AND visibility != 'PRIVATE')
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncMemos(): List<MemoEntity>

    @Query(
        """
        SELECT * FROM memos
        WHERE (syncStatus != 'SYNCED'
           OR (remoteId IS NULL AND visibility != 'PRIVATE'))
          AND coupleId = :coupleId
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncMemosForCouple(coupleId: String): List<MemoEntity>

    @Query(
        """
        SELECT * FROM memos
        WHERE remoteId = :remoteId
        LIMIT 1
        """
    )
    suspend fun findMemoByRemoteId(remoteId: String): MemoEntity?

    @Query(
        """
        SELECT * FROM memos
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findMemoByRemoteIdForCouple(remoteId: String, coupleId: String): MemoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity): Long

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Query(
        """
        UPDATE memos
        SET isCompleted = :isCompleted,
            syncStatus = CASE WHEN visibility = 'PRIVATE' THEN syncStatus ELSE 'PENDING_UPDATE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setMemoCompleted(id: Long, isCompleted: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE memos
        SET isDeleted = 1,
            syncStatus = CASE WHEN visibility = 'PRIVATE' AND remoteId IS NULL THEN syncStatus ELSE 'PENDING_DELETE' END,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteMemo(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE memos
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markMemoSynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE memos
        SET remoteId = NULL,
            coupleId = NULL,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markMemoPrivateRevokeSynced(
        id: Long,
        ownerUserId: String,
        updatedAt: Long
    )

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
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY startEpochDay DESC
        """
    )
    fun observeCareCyclesForCouple(coupleId: String): Flow<List<CareCycleEntity>>

    @Query(
        """
        SELECT * FROM care_cycles
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCareCycles(): List<CareCycleEntity>

    @Query(
        """
        SELECT * FROM care_cycles
        WHERE coupleId = :coupleId
          AND (syncStatus != 'SYNCED' OR remoteId IS NULL)
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCareCyclesForCouple(coupleId: String): List<CareCycleEntity>

    @Query("SELECT * FROM care_cycles WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findCareCycleByRemoteId(remoteId: String): CareCycleEntity?

    @Query(
        """
        SELECT * FROM care_cycles
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findCareCycleByRemoteIdForCouple(remoteId: String, coupleId: String): CareCycleEntity?

    @Query(
        """
        SELECT * FROM care_cycles
        WHERE startEpochDay = :startEpochDay AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findCareCycleByStart(
        startEpochDay: Long,
        coupleId: String
    ): CareCycleEntity?

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
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        """
    )
    suspend fun softDeleteAllCareCyclesForCouple(coupleId: String, updatedAt: Long)

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
