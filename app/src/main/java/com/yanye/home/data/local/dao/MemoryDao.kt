package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query(
        """
        SELECT * FROM memories
        WHERE isDeleted = 0
        ORDER BY dateEpochDay DESC, createdAt DESC
        """
    )
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY dateEpochDay DESC, createdAt DESC
        """
    )
    fun observeMemoriesForCouple(coupleId: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncMemories(): List<MemoryEntity>

    @Query(
        """
        SELECT * FROM memories
        WHERE (syncStatus != 'SYNCED'
           OR remoteId IS NULL)
          AND coupleId = :coupleId
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncMemoriesForCouple(coupleId: String): List<MemoryEntity>

    @Query(
        """
        SELECT * FROM memories
        WHERE remoteId = :remoteId
        LIMIT 1
        """
    )
    suspend fun findByRemoteId(remoteId: String): MemoryEntity?

    @Query(
        """
        SELECT * FROM memories
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findByRemoteIdForCouple(remoteId: String, coupleId: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query(
        """
        UPDATE memories
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

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()

}
