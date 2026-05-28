package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.AnniversaryRepository
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseAnniversarySyncService(
    context: Context,
    private val anniversaryRepository: AnniversaryRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<AnniversarySyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_ANNIVERSARIES_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching AnniversarySyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase HTTP 地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pending = anniversaryRepository.pendingSyncAnniversaries()
                    .filter { SyncRules.shouldSyncSharedVisibility(it.visibility) }
                val pendingPayloads = pending.map { anniversary ->
                    val remoteId = anniversary.remoteId ?: SyncRules.remoteId("anniversary", identity, anniversary.id)
                    PendingAnniversaryPayload(anniversary = anniversary, remoteId = remoteId)
                }
                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put(
                            "pendingAnniversaries",
                            JSONArray(pendingPayloads.map { it.anniversary.toJson(it.remoteId) })
                        )
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 同步失败"))
                }

                val remoteItems = response.optJSONArray("anniversaries") ?: JSONArray()
                pendingPayloads.forEach { payload ->
                    val local = payload.anniversary
                    anniversaryRepository.markSynced(
                        localId = local.id,
                        remoteId = payload.remoteId,
                        coupleId = identity.coupleId,
                        ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                for (index in 0 until remoteItems.length()) {
                    val remote = remoteItems.getJSONObject(index).toAnniversary()
                    anniversaryRepository.saveRemoteAnniversary(remote)
                }

                AnniversarySyncResult(
                    uploaded = pending.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

private data class PendingAnniversaryPayload(
    val anniversary: Anniversary,
    val remoteId: String
)

private fun Anniversary.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("name", name)
        .put("dateEpochDay", dateEpochDay)
        .put("type", type.storageValue)
        .put("displayMode", displayMode.storageValue)
        .put("reminderDaysBefore", reminderDaysBefore)
        .put("note", note)
        .put("coverImageUri", coverImageUri)
        .put("giftWishLinkEnabled", giftWishLinkEnabled)
        .put("celebrationArchiveEnabled", celebrationArchiveEnabled)
        .put("createdBy", createdBy)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("visibility", visibility.storageValue)
        .put("sharedWithPartner", sharedWithPartner)
        .put("lockedUntilEpochDay", lockedUntilEpochDay)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)
        .put("showOnHome", showOnHome)
        .put("homeSortOrder", homeSortOrder)

private fun JSONObject.toAnniversary(): Anniversary {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return Anniversary(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        name = optString("name"),
        dateEpochDay = optLong("dateEpochDay"),
        type = AnniversaryType.fromStorageValue(optString("type")),
        displayMode = AnniversaryDisplayMode.fromStorageValue(optString("displayMode")),
        reminderDaysBefore = optInt("reminderDaysBefore", 7),
        note = optString("note"),
        coverImageUri = optString("coverImageUri").ifBlank { null },
        giftWishLinkEnabled = optBoolean("giftWishLinkEnabled", true),
        celebrationArchiveEnabled = optBoolean("celebrationArchiveEnabled", true),
        createdBy = optString("createdBy").ifBlank { null },
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        visibility = Visibility.fromStorageValue(optString("visibility")),
        sharedWithPartner = optBoolean("sharedWithPartner", true),
        lockedUntilEpochDay = if (isNull("lockedUntilEpochDay")) null else optLong("lockedUntilEpochDay"),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt,
        showOnHome = optBoolean("showOnHome", false),
        homeSortOrder = optInt("homeSortOrder", 100)
    )
}
