package com.yanye.home.data.sync

import android.content.Context
import com.yanye.home.data.session.SessionSettings
import java.util.UUID

data class SyncIdentity(
    val localUserId: String,
    val coupleId: String
)

class SyncSettings(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext.getSharedPreferences(
        "yanye_sync_settings",
        Context.MODE_PRIVATE
    )
    private val sessionSettings = SessionSettings(appContext)

    fun identity(): SyncIdentity {
        sessionSettings.loadSession()?.let { session ->
            return SyncIdentity(
                localUserId = session.userId,
                coupleId = session.currentSpaceId?.takeIf { it.isNotBlank() }
                    ?: "solo_${session.userId}"
            )
        }
        val localUserId = preferences.getString(KEY_LOCAL_USER_ID, null)
            ?: UUID.randomUUID().toString().also { value ->
                preferences.edit().putString(KEY_LOCAL_USER_ID, value).apply()
            }
        val coupleId = preferences.getString(KEY_COUPLE_ID, DEFAULT_COUPLE_ID) ?: DEFAULT_COUPLE_ID
        return SyncIdentity(
            localUserId = localUserId,
            coupleId = coupleId
        )
    }

    fun setCoupleId(coupleId: String) {
        preferences.edit()
            .putString(KEY_COUPLE_ID, coupleId.trim().ifBlank { DEFAULT_COUPLE_ID })
            .apply()
    }

    private companion object {
        const val KEY_LOCAL_USER_ID = "local_user_id"
        const val KEY_COUPLE_ID = "couple_id"
        const val DEFAULT_COUPLE_ID = "yanyehome"
    }
}
