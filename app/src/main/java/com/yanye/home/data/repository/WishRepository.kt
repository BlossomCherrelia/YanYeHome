package com.yanye.home.data.repository

import com.yanye.home.data.local.dao.WishDao
import com.yanye.home.data.local.entity.WishEntity
import com.yanye.home.data.sync.SyncSettings
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish
import com.yanye.home.domain.model.WishCategory
import com.yanye.home.domain.model.WishPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WishRepository(
    private val wishDao: WishDao,
    private val syncSettings: SyncSettings
) {
    fun observeWishes(): Flow<List<Wish>> =
        wishDao.observeWishesForCouple(syncSettings.identity().coupleId)
            .map { wishes -> wishes.map(WishEntity::toDomain) }

    fun observeWish(id: Long): Flow<Wish?> =
        wishDao.observeWish(id)
            .map { wish -> wish?.toDomain() }

    suspend fun saveWish(wish: Wish): Long {
        val now = System.currentTimeMillis()
        val syncStatus = when {
            wish.visibility == Visibility.Private && !wish.remoteId.isNullOrBlank() -> SyncStatus.PendingDelete
            wish.visibility == Visibility.Private -> SyncStatus.Synced
            wish.remoteId.isNullOrBlank() -> SyncStatus.PendingCreate
            else -> SyncStatus.PendingUpdate
        }
        val identity = syncSettings.identity()
        val scopedWish = wish.copy(
            coupleId = wish.coupleId ?: identity.coupleId,
            ownerUserId = wish.ownerUserId ?: identity.localUserId
        )
        val entity = scopedWish.toEntity(
            createdAt = wish.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            syncStatus = syncStatus
        )

        return if (wish.id == 0L) {
            wishDao.insertWish(entity)
        } else {
            wishDao.updateWish(entity)
            wish.id
        }
    }

    suspend fun setWishCompleted(id: Long, isCompleted: Boolean) {
        wishDao.setWishCompleted(
            id = id,
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun pendingSyncWishes(): List<Wish> =
        wishDao.pendingSyncWishesForCouple(syncSettings.identity().coupleId)
            .map(WishEntity::toDomain)

    suspend fun saveRemoteWish(wish: Wish): Long {
        if (wish.coupleId != syncSettings.identity().coupleId) return 0L
        val now = System.currentTimeMillis()
        val entity = wish.toEntity(
            createdAt = wish.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = wish.updatedAt.takeIf { it > 0 } ?: now,
            syncStatus = SyncStatus.Synced
        )
        val identity = syncSettings.identity()
        val local = wish.remoteId?.let { wishDao.findByRemoteIdForCouple(it, identity.coupleId) }
        if (local == null && wish.isDeleted) {
            return 0L
        }

        return if (local == null) {
            wishDao.insertWish(entity)
        } else {
            wishDao.updateWish(entity.copy(id = local.id))
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
        wishDao.markSynced(
            id = localId,
            remoteId = remoteId,
            coupleId = coupleId,
            ownerUserId = ownerUserId,
            remoteUpdatedAt = remoteUpdatedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun markPrivateRevokeSynced(
        localId: Long,
        ownerUserId: String
    ) {
        wishDao.markPrivateRevokeSynced(
            id = localId,
            ownerUserId = ownerUserId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteWish(id: Long) {
        wishDao.softDeleteWish(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
    }

}

private fun WishEntity.toDomain(): Wish =
    Wish(
        id = id,
        title = title,
        category = WishCategory.fromStorageValue(category),
        visibility = Visibility.fromStorageValue(visibility),
        revealAfterEpochDay = revealAfterEpochDay,
        budgetCents = budgetCents,
        locationName = locationName,
        priority = WishPriority.fromStorageValue(priority),
        targetDateEpochDay = targetDateEpochDay,
        note = note,
        preparationItems = preparationItems,
        coverImageUri = coverImageUri,
        isCompleted = isCompleted,
        scheduleReady = scheduleReady,
        linkedScheduleId = linkedScheduleId,
        giftCandidateForAnniversaryId = giftCandidateForAnniversaryId,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sharedWithPartner = sharedWithPartner,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = SyncStatus.fromStorageValue(syncStatus),
        remoteUpdatedAt = remoteUpdatedAt
    )

private fun Wish.toEntity(
    createdAt: Long,
    updatedAt: Long,
    syncStatus: SyncStatus = this.syncStatus
): WishEntity =
    WishEntity(
        id = id,
        title = title,
        category = category.storageValue,
        visibility = visibility.storageValue,
        revealAfterEpochDay = revealAfterEpochDay,
        budgetCents = budgetCents,
        locationName = locationName,
        priority = priority.storageValue,
        targetDateEpochDay = targetDateEpochDay,
        note = note,
        preparationItems = preparationItems,
        coverImageUri = coverImageUri,
        isCompleted = isCompleted,
        scheduleReady = scheduleReady,
        linkedScheduleId = linkedScheduleId,
        giftCandidateForAnniversaryId = giftCandidateForAnniversaryId,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sharedWithPartner = sharedWithPartner,
        isDeleted = isDeleted,
        remoteId = remoteId,
        coupleId = coupleId,
        ownerUserId = ownerUserId,
        syncStatus = syncStatus.storageValue,
        remoteUpdatedAt = remoteUpdatedAt
    )
