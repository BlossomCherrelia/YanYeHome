package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.repository.MemoRepository
import com.yanye.home.domain.model.Memo
import com.yanye.home.domain.model.MemoCategory
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CloudBaseMemoSyncService(
    context: Context,
    private val memoRepository: MemoRepository,
    private val syncSettings: SyncSettings = SyncSettings(context),
    private val httpClient: CloudBaseHttpClient = CloudBaseHttpClient()
) {
    suspend fun syncOnce(): Result<MemoSyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = CloudBaseConfig.SYNC_MEMOS_URL.trim()
                if (endpoint.isBlank()) {
                    return@runCatching MemoSyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "CloudBase 备忘录同步地址未配置"
                    )
                }

                val identity = syncSettings.identity()
                val pendingPayloads = memoRepository.pendingSyncMemos()
                    .filter { memo -> shouldUploadMemo(memo) }
                    .map { memo ->
                        val remoteId = memo.remoteId
                            ?: SyncRules.remoteId("memo", identity, memo.id)
                        PendingMemoPayload(
                            memo = memo,
                            remoteId = remoteId,
                            isPrivateRevoke = isPrivateRevoke(memo)
                        )
                    }

                val response = httpClient.postJson(
                    endpoint = endpoint,
                    body = JSONObject()
                        .put("envId", CloudBaseConfig.ENV_ID)
                        .put("coupleId", identity.coupleId)
                        .put("userId", identity.localUserId)
                        .put("pendingMemos", JSONArray(pendingPayloads.map { it.toJson() }))
                )

                if (!response.optBoolean("ok")) {
                    error(response.optString("error", "CloudBase 备忘录同步失败"))
                }

                val remoteItems = response.optJSONArray("memos") ?: JSONArray()
                pendingPayloads.forEach { payload ->
                    val local = payload.memo
                    if (payload.isPrivateRevoke) {
                        memoRepository.markMemoPrivateRevokeSynced(
                            localId = local.id,
                            ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity)
                        )
                    } else {
                        memoRepository.markMemoSynced(
                            localId = local.id,
                            remoteId = payload.remoteId,
                            coupleId = identity.coupleId,
                            ownerUserId = SyncRules.ownerUserId(local.ownerUserId, identity),
                            remoteUpdatedAt = System.currentTimeMillis()
                        )
                    }
                }
                for (index in 0 until remoteItems.length()) {
                    memoRepository.saveRemoteMemo(remoteItems.getJSONObject(index).toMemo())
                }

                MemoSyncResult(
                    uploaded = pendingPayloads.size,
                    downloaded = remoteItems.length(),
                    skippedReason = null
                )
            }
        }
}

data class MemoSyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private data class PendingMemoPayload(
    val memo: Memo,
    val remoteId: String,
    val isPrivateRevoke: Boolean
) {
    fun toJson(): JSONObject =
        if (memo.syncStatus == SyncStatus.PendingDelete) {
            memo.toDeletedJson(remoteId)
        } else {
            memo.toJson(remoteId)
        }
}

private fun shouldUploadMemo(memo: Memo): Boolean {
    if (memo.syncStatus == SyncStatus.PendingDelete) {
        return memo.remoteId != null || memo.visibility != Visibility.Private
    }
    return memo.visibility != Visibility.Private && memo.sharedWithPartner
}

private fun isPrivateRevoke(memo: Memo): Boolean =
    memo.syncStatus == SyncStatus.PendingDelete &&
        memo.visibility == Visibility.Private &&
        !memo.isDeleted &&
        memo.remoteId != null

private fun Memo.toDeletedJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("title", "")
        .put("content", "")
        .put("category", category.storageValue)
        .put("checklistItems", "")
        .put("imageUris", "")
        .put("dueLabel", dueLabel)
        .put("reminderAtMillis", null)
        .put("reminderEnabled", false)
        .put("notificationChannelKey", notificationChannelKey)
        .put("linkedScheduleId", null)
        .put("visibility", Visibility.Private.storageValue)
        .put("sharedWithPartner", false)
        .put("isCompleted", isCompleted)
        .put("showOnHome", false)
        .put("homeSortOrder", homeSortOrder)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", true)
        .put("ownerUserId", ownerUserId)

private fun Memo.toJson(remoteId: String): JSONObject =
    JSONObject()
        .put("remoteId", remoteId)
        .put("title", title)
        .put("content", content)
        .put("category", category.storageValue)
        .put("checklistItems", checklistItems)
        .put("imageUris", imageUris)
        .put("dueLabel", dueLabel)
        .put("reminderAtMillis", reminderAtMillis)
        .put("reminderEnabled", reminderEnabled)
        .put("notificationChannelKey", notificationChannelKey)
        .put("linkedScheduleId", linkedScheduleId)
        .put("visibility", visibility.storageValue)
        .put("sharedWithPartner", sharedWithPartner)
        .put("isCompleted", isCompleted)
        .put("showOnHome", showOnHome)
        .put("homeSortOrder", homeSortOrder)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("isDeleted", isDeleted)
        .put("ownerUserId", ownerUserId)

private fun JSONObject.toMemo(): Memo {
    val remoteId = optString("remoteId").ifBlank { optString("_id") }
    val remoteUpdatedAt = optLong("remoteUpdatedAt", optLong("updatedAt", 0L))
    return Memo(
        remoteId = remoteId,
        coupleId = optString("coupleId").ifBlank { null },
        ownerUserId = optString("ownerUserId").ifBlank { null },
        title = optString("title"),
        content = optString("content"),
        category = MemoCategory.fromStorageValue(optString("category")),
        checklistItems = optString("checklistItems"),
        imageUris = optString("imageUris"),
        dueLabel = optString("dueLabel"),
        reminderAtMillis = if (isNull("reminderAtMillis")) null else optLong("reminderAtMillis"),
        reminderEnabled = optBoolean("reminderEnabled", false),
        notificationChannelKey = optString("notificationChannelKey").ifBlank { "memo_reminder" },
        linkedScheduleId = if (isNull("linkedScheduleId")) null else optLong("linkedScheduleId"),
        visibility = Visibility.fromStorageValue(optString("visibility")),
        sharedWithPartner = optBoolean("sharedWithPartner", true),
        isCompleted = optBoolean("isCompleted", false),
        showOnHome = optBoolean("showOnHome", false),
        homeSortOrder = optInt("homeSortOrder", 100),
        createdAt = optLong("createdAt", remoteUpdatedAt),
        updatedAt = optLong("updatedAt", remoteUpdatedAt),
        isDeleted = optBoolean("isDeleted", false),
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}
