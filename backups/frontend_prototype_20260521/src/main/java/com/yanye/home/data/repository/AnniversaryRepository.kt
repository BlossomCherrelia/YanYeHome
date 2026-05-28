package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.AnniversaryDao
import com.yanye.home.data.local.entity.AnniversaryEntity
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnniversaryRepository(
    private val anniversaryDao: AnniversaryDao
) {
    fun observeAnniversaries(): Flow<List<Anniversary>> =
        anniversaryDao.observeAnniversaries()
            .map { anniversaries -> anniversaries.map(AnniversaryEntity::toDomain) }

    fun observeAnniversary(id: Long): Flow<Anniversary?> =
        anniversaryDao.observeAnniversary(id)
            .map { anniversary -> anniversary?.toDomain() }

    suspend fun pendingSyncAnniversaries(): List<Anniversary> =
        anniversaryDao.pendingSyncAnniversaries()
            .map(AnniversaryEntity::toDomain)

    suspend fun saveAnniversary(anniversary: Anniversary): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (anniversary.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val entity = anniversary.copy(syncStatus = syncStatus).toEntity(
            createdAt = anniversary.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now
        )

        return if (anniversary.id == 0L) {
            anniversaryDao.insertAnniversary(entity)
        } else {
            anniversaryDao.updateAnniversary(entity)
            anniversary.id
        }
    }

    suspend fun saveRemoteAnniversary(anniversary: Anniversary): Long {
        val now = System.currentTimeMillis()
        val entity = anniversary.toEntity(
            createdAt = anniversary.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = anniversary.updatedAt.takeIf { it > 0 } ?: now
        )
        val local = anniversary.remoteId?.let { anniversaryDao.findByRemoteId(it) }

        return if (local == null) {
            anniversaryDao.insertAnniversary(entity)
        } else {
            anniversaryDao.updateAnniversary(entity.copy(id = local.id))
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
        anniversaryDao.markSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteAnniversary(id: Long) {
        anniversaryDao.softDeleteAnniversary(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }
}

private fun AnniversaryEntity.toDomain(): Anniversary =
    Anniversary(
        id = id,
        name = name,
        dateEpochDay = dateEpochDay,
        type = AnniversaryType.fromStorageValue(type),
        displayMode = AnniversaryDisplayMode.fromStorageValue(displayMode),
        reminderDaysBefore = reminderDaysBefore,
        note = note,
        coverImageUri = coverImageUri,
        giftWishLinkEnabled = giftWishLinkEnabled,
        celebrationArchiveEnabled = celebrationArchiveEnabled,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        visibility = Visibility.fromStorageValue(visibility),
        sharedWithPartner = sharedWithPartner,
        lockedUntilEpochDay = lockedUntilEpochDay,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun Anniversary.toEntity(
    createdAt: Long,
    updatedAt: Long
): AnniversaryEntity =
    AnniversaryEntity(
        id = id,
        name = name,
        dateEpochDay = dateEpochDay,
        type = type.storageValue,
        displayMode = displayMode.storageValue,
        reminderDaysBefore = reminderDaysBefore,
        note = note,
        coverImageUri = coverImageUri,
        giftWishLinkEnabled = giftWishLinkEnabled,
        celebrationArchiveEnabled = celebrationArchiveEnabled,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        visibility = visibility.storageValue,
        sharedWithPartner = sharedWithPartner,
        lockedUntilEpochDay = lockedUntilEpochDay,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )
