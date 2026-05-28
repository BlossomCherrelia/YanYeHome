package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.AnniversaryDao
import com.yanye.home.data.local.entity.AnniversaryEntity
import com.yanye.home.data.sync.SyncSettings
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class AnniversaryRepository(
    private val anniversaryDao: AnniversaryDao,
    private val syncSettings: SyncSettings
) {
    fun observeAnniversaries(): Flow<List<Anniversary>> =
        anniversaryDao.observeAnniversariesForCouple(syncSettings.identity().coupleId)
            .map { anniversaries -> anniversaries.map(AnniversaryEntity::toDomain) }

    fun observeAnniversary(id: Long): Flow<Anniversary?> =
        anniversaryDao.observeAnniversary(id)
            .map { anniversary -> anniversary?.toDomain() }

    suspend fun pendingSyncAnniversaries(): List<Anniversary> =
        anniversaryDao.pendingSyncAnniversariesForCouple(syncSettings.identity().coupleId)
            .map(AnniversaryEntity::toDomain)

    suspend fun ensureDefaultRelationshipAnniversary() {
        val identity = syncSettings.identity()
        if (anniversaryDao.findActiveAnniversaryByNameForCouple(
                DEFAULT_RELATIONSHIP_ANNIVERSARY_NAME,
                identity.coupleId
            ) != null
        ) return

        val now = System.currentTimeMillis()
        anniversaryDao.insertAnniversary(
            AnniversaryEntity(
                name = DEFAULT_RELATIONSHIP_ANNIVERSARY_NAME,
                dateEpochDay = LocalDate.of(2025, 1, 1).toEpochDay(),
                type = AnniversaryType.Relationship.storageValue,
                displayMode = AnniversaryDisplayMode.CountUp.storageValue,
                reminderDaysBefore = 7,
                note = "",
                giftWishLinkEnabled = true,
                celebrationArchiveEnabled = true,
                createdAt = now,
                updatedAt = now,
                coupleId = identity.coupleId,
                ownerUserId = identity.localUserId,
                visibility = Visibility.Shared.storageValue,
                sharedWithPartner = true,
                syncStatus = SyncStatus.PendingCreate.storageValue,
                showOnHome = true,
                homeSortOrder = 0
            )
        )
    }

    suspend fun saveAnniversary(anniversary: Anniversary): Long {
        val now = System.currentTimeMillis()
        val syncStatus = if (anniversary.remoteId.isNullOrBlank()) {
            SyncStatus.PendingCreate
        } else {
            SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val entity = anniversary.copy(
            syncStatus = syncStatus,
            coupleId = anniversary.coupleId ?: identity.coupleId,
            ownerUserId = anniversary.ownerUserId ?: identity.localUserId
        ).toEntity(
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
        if (anniversary.coupleId != syncSettings.identity().coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = anniversary.toEntity(
            createdAt = anniversary.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = anniversary.updatedAt.takeIf { it > 0 } ?: now
        )
        val identity = syncSettings.identity()
        val local = anniversary.remoteId?.let { anniversaryDao.findByRemoteIdForCouple(it, identity.coupleId) }

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
        remoteUpdatedAt = remoteUpdatedAt,
        showOnHome = showOnHome,
        homeSortOrder = homeSortOrder
    )

private const val DEFAULT_RELATIONSHIP_ANNIVERSARY_NAME = "在一起"

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
        remoteUpdatedAt = remoteUpdatedAt,
        showOnHome = showOnHome,
        homeSortOrder = homeSortOrder
    )
