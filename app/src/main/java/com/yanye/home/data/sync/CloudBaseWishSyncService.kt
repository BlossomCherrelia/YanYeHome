package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.WishRepository
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish
import com.yanye.home.domain.model.WishCategory
import com.yanye.home.domain.model.WishPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class CloudBaseWishSyncService(
    context: Context,
    private val wishRepository: WishRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<WishSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_WISHES_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching WishSyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 愿望同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val todayEpochDay = LocalDate.now().toEpochDay()
                val pendingPayloads = wishRepository.pendingSyncWishes()
                    .filter { SyncRules.shouldUploadWish(it, todayEpochDay) }
                    .map { wish ->
                        val remoteId = wish.remoteId ?: SyncRules.remoteId("wish", identity, wish.id)
                        PendingWishPayload(
                            wish = wish,
                            remoteId = remoteId,
                            isPrivateRevoke = SyncRules.isPrivateRevoke(wish)
                        )
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put("todayEpochDay", todayEpochDay)
                        .put("pendingWishes", JSONArray(pendingPayloads.map { it.toJson() }))
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 愿望同步失败"))
                }

                val remoteItems = response.optJSONArray("wishes") ?: JSONArray()
                pendingPayloads.forEach { payload ->
                    val local = payload.wish
                    if (payload.isPrivateRevoke) {
                        wishRepository.markPrivateRevokeSynced(
                            localId = local.id,
                            ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity)
                        )
                    } else {
                        wishRepository.markSynced(
                            localId = local.id,
                            remoteId = payload.remoteId,
                            coupleId = identity.coupleId,
                            ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                            remoteUpdatedAt = System.currentTimeMillis()
                        )
                    }
                }
                for (index in 0 until remoteItems.length()) {
                    val remote = remoteItems.getJSONObject(index).toWish()
                    wishRepository.saveRemoteWish(remote)
                }

                WishSyncResult(
                    uploaded = pendingPayloads.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

data class WishSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingWishPayload(
    val wish: Wish,
    val remoteId: String,
    val isPrivateRevoke: Boolean
) {
    fun toJson(): JSONObject =
        if (wish.syncStatus == SyncStatus.PendingDelete) {
            wish.toDeletedJson(remoteId)
        } else {
            wish.toJson(remoteId)
        }
}

private fun Wish.toDeletedJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("title", "")
        .put("category", category.storageValue)
        .put("visibility", Visibility.Private.storageValue)
        .put("revealAfterEpochDay", null)
        .put("budgetCents", null)
        .put("locationName", "")
        .put("priority", priority.storageValue)
        .put("targetDateEpochDay", null)
        .put("note", "")
        .put("preparationItems", "")
        .put("coverImageUri", null)
        .put("isCompleted", isCompleted)
        .put("scheduleReady", false)
        .put("linkedScheduleId", null)
        .put("giftCandidateForAnniversaryId", null)
        .put("createdBy", createdBy)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("sharedWithPartner", false)
        .put("isDeleted", true)
        .put("ownerUserId", ownerUserId)

private fun Wish.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("title", title)
        .put("category", category.storageValue)
        .put("visibility", visibility.storageValue)
        .put("revealAfterEpochDay", revealAfterEpochDay)
        .put("budgetCents", budgetCents)
        .put("locationName", locationName)
        .put("priority", priority.storageValue)
        .put("targetDateEpochDay", targetDateEpochDay)
        .put("note", note)
        .put("preparationItems", preparationItems)
        .put("coverImageUri", coverImageUri)
        .put("isCompleted", isCompleted)
        .put("scheduleReady", scheduleReady)
        .put("linkedScheduleId", linkedScheduleId)
        .put("giftCandidateForAnniversaryId", giftCandidateForAnniversaryId)
        .put("createdBy", createdBy)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("sharedWithPartner", sharedWithPartner)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toWish(): Wish {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return Wish(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        title = optString("title"),
        category = WishCategory.fromStorageValue(optString("category")),
        visibility = Visibility.fromStorageValue(optString("visibility")),
        revealAfterEpochDay = if (isNull("revealAfterEpochDay")) null else optLong("revealAfterEpochDay"),
        budgetCents = if (isNull("budgetCents")) null else optLong("budgetCents"),
        locationName = optString("locationName"),
        priority = WishPriority.fromStorageValue(optString("priority")),
        targetDateEpochDay = if (isNull("targetDateEpochDay")) null else optLong("targetDateEpochDay"),
        note = optString("note"),
        preparationItems = optString("preparationItems"),
        coverImageUri = optString("coverImageUri").ifBlank { null },
        isCompleted = optBoolean("isCompleted", false),
        scheduleReady = optBoolean("scheduleReady", true),
        linkedScheduleId = if (isNull("linkedScheduleId")) null else optLong("linkedScheduleId"),
        giftCandidateForAnniversaryId = if (isNull("giftCandidateForAnniversaryId")) null else optLong("giftCandidateForAnniversaryId"),
        createdBy = optString("createdBy").ifBlank { null },
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        sharedWithPartner = optBoolean("sharedWithPartner", true),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
