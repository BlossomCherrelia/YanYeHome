package com.yanye.home.data.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.yanye.home.data.repository.AnniversaryRepository
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.domain.model.SyncStatus
import com.yanye.home.domain.model.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnniversarySyncService(
    private val context: Context,
    private val anniversaryRepository: AnniversaryRepository,
    private val syncSettings: SyncSettings = SyncSettings(context)
) {
    suspend fun syncOnce(): Result<AnniversarySyncResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!FirebaseAvailability.isConfigured(context)) {
                    return@runCatching AnniversarySyncResult(
                        uploaded = 0,
                        downloaded = 0,
                        skippedReason = "Firebase 未配置 google-services.json"
                    )
                }

                val identity = syncSettings.identity()
                val userId = ensureFirebaseUserId(identity.localUserId)
                val firestore = FirebaseFirestore.getInstance()
                val collection = firestore
                    .collection("couples")
                    .document(identity.coupleId)
                    .collection("anniversaries")

                var uploaded = 0
                anniversaryRepository.pendingSyncAnniversaries()
                    .filter { it.visibility == Visibility.Shared }
                    .forEach { anniversary ->
                        val remoteId = anniversary.remoteId ?: collection.document().id
                        val remoteUpdatedAt = System.currentTimeMillis()
                        awaitFirebaseTask(
                            label = "上传纪念日到 Firestore",
                            task =
                            collection.document(remoteId)
                                .set(anniversary.toRemoteMap(remoteId, identity.coupleId, userId, remoteUpdatedAt))
                        )
                        anniversaryRepository.markSynced(
                            localId = anniversary.id,
                            remoteId = remoteId,
                            coupleId = identity.coupleId,
                            ownerUserId = anniversary.ownerUserId ?: userId,
                            remoteUpdatedAt = remoteUpdatedAt
                        )
                        uploaded += 1
                    }

                val snapshot = awaitFirebaseTask(
                    label = "从 Firestore 拉取纪念日",
                    task = collection.get()
                )
                var downloaded = 0
                snapshot.documents.forEach { document ->
                    val remote = document.data?.toAnniversary(document.id) ?: return@forEach
                    anniversaryRepository.saveRemoteAnniversary(remote)
                    downloaded += 1
                }

                AnniversarySyncResult(
                    uploaded = uploaded,
                    downloaded = downloaded,
                    skippedReason = null
                )
            }
        }

    private fun ensureFirebaseUserId(fallbackUserId: String): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        return runCatching {
            awaitFirebaseTask(
                label = "Firebase 匿名登录",
                task = auth.signInAnonymously()
            ).user?.uid
        }.getOrNull() ?: fallbackUserId
    }

    private fun <T> awaitFirebaseTask(
        label: String,
        task: Task<T>
    ): T =
        try {
            Tasks.await(task, FIREBASE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (error: TimeoutException) {
            throw IllegalStateException("$label 超时：当前网络可能无法访问 Firebase，或模拟器/手机没有走可访问 Google 服务的网络。", error)
        }

    private companion object {
        const val FIREBASE_TIMEOUT_SECONDS = 15L
    }
}

data class AnniversarySyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skippedReason: String?
)

private fun Anniversary.toRemoteMap(
    remoteId: String,
    coupleId: String,
    userId: String,
    remoteUpdatedAt: Long
): Map<String, Any?> =
    mapOf(
        "remoteId" to remoteId,
        "coupleId" to coupleId,
        "ownerUserId" to (ownerUserId ?: userId),
        "name" to name,
        "dateEpochDay" to dateEpochDay,
        "type" to type.storageValue,
        "displayMode" to displayMode.storageValue,
        "reminderDaysBefore" to reminderDaysBefore,
        "note" to note,
        "coverImageUri" to coverImageUri,
        "giftWishLinkEnabled" to giftWishLinkEnabled,
        "celebrationArchiveEnabled" to celebrationArchiveEnabled,
        "createdBy" to (createdBy ?: userId),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "visibility" to visibility.storageValue,
        "sharedWithPartner" to sharedWithPartner,
        "lockedUntilEpochDay" to lockedUntilEpochDay,
        "isDeleted" to isDeleted,
        "remoteUpdatedAt" to remoteUpdatedAt
    )

private fun Map<String, Any?>.toAnniversary(remoteId: String): Anniversary {
    val remoteUpdatedAt = longValue("remoteUpdatedAt") ?: longValue("updatedAt") ?: 0L
    return Anniversary(
        remoteId = remoteId,
        coupleId = stringValue("coupleId"),
        ownerUserId = stringValue("ownerUserId"),
        name = stringValue("name").orEmpty(),
        dateEpochDay = longValue("dateEpochDay") ?: 0L,
        type = AnniversaryType.fromStorageValue(stringValue("type").orEmpty()),
        displayMode = AnniversaryDisplayMode.fromStorageValue(stringValue("displayMode").orEmpty()),
        reminderDaysBefore = longValue("reminderDaysBefore")?.toInt() ?: 7,
        note = stringValue("note").orEmpty(),
        coverImageUri = stringValue("coverImageUri"),
        giftWishLinkEnabled = booleanValue("giftWishLinkEnabled") ?: true,
        celebrationArchiveEnabled = booleanValue("celebrationArchiveEnabled") ?: true,
        createdBy = stringValue("createdBy"),
        createdAt = longValue("createdAt") ?: remoteUpdatedAt,
        updatedAt = longValue("updatedAt") ?: remoteUpdatedAt,
        visibility = Visibility.fromStorageValue(stringValue("visibility").orEmpty()),
        sharedWithPartner = booleanValue("sharedWithPartner") ?: true,
        lockedUntilEpochDay = longValue("lockedUntilEpochDay"),
        isDeleted = booleanValue("isDeleted") ?: false,
        syncStatus = SyncStatus.Synced,
        remoteUpdatedAt = remoteUpdatedAt
    )
}

private fun Map<String, Any?>.stringValue(key: String): String? =
    this[key] as? String

private fun Map<String, Any?>.longValue(key: String): Long? =
    when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        else -> null
    }

private fun Map<String, Any?>.booleanValue(key: String): Boolean? =
    this[key] as? Boolean
