package com.yanye.home.data.sync

import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish

object SyncRules {
    fun remoteId(prefix: String, identity: SyncIdentity, localId: Long): String =
        remoteIdFromParts(prefix, identity.coupleId, identity.localUserId, localId.toString())

    fun remoteIdFromParts(prefix: String, vararg parts: String): String =
        buildString {
            append(prefix)
            parts.forEach { part ->
                append('_')
                append(part.toByteArray(Charsets.UTF_8).joinToString(separator = "") { byte ->
                    "%02x".format(byte.toInt() and 0xff)
                })
            }
        }

    fun ownerUserId(ownerUserId: String?, identity: SyncIdentity): String =
        ownerUserId ?: identity.localUserId

    fun shouldSyncSharedVisibility(visibility: Visibility): Boolean =
        visibility == Visibility.Shared

    fun shouldUploadWish(wish: Wish, todayEpochDay: Long): Boolean {
        if (wish.syncStatus == SyncStatus.PendingDelete) {
            return wish.remoteId != null || wish.visibility != Visibility.Private
        }
        if (wish.visibility == Visibility.Private) return false
        if (wish.visibility == Visibility.RevealAfterDate) {
            return wish.revealAfterEpochDay != null && wish.revealAfterEpochDay <= todayEpochDay
        }
        return true
    }

    fun isPrivateRevoke(wish: Wish): Boolean =
        wish.syncStatus == SyncStatus.PendingDelete &&
            wish.visibility == Visibility.Private &&
            !wish.isDeleted &&
            wish.remoteId != null
}
