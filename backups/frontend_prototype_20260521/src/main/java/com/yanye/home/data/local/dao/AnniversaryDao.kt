package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.AnniversaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnniversaryDao {
    @Query(
        """
        SELECT * FROM anniversaries
        WHERE isDeleted = 0
        ORDER BY dateEpochDay ASC, createdAt ASC
        """
    )
    fun observeAnniversaries(): Flow<List<AnniversaryEntity>>

    @Query(
        """
        SELECT * FROM anniversaries
        WHERE id = :id AND isDeleted = 0
        LIMIT 1
        """
    )
    fun observeAnniversary(id: Long): Flow<AnniversaryEntity?>

    @Query(
        """
        SELECT * FROM anniversaries
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncAnniversaries(): List<AnniversaryEntity>

    @Query(
        """
        SELECT * FROM anniversaries
        WHERE remoteId = :remoteId
        LIMIT 1
        """
    )
    suspend fun findByRemoteId(remoteId: String): AnniversaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnniversary(anniversary: AnniversaryEntity): Long

    @Update
    suspend fun updateAnniversary(anniversary: AnniversaryEntity)

    @Query(
        """
        UPDATE anniversaries
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
        UPDATE anniversaries
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteAnniversary(id: Long, updatedAt: Long)
}
