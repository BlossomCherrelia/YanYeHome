package com.yanye.home.data.session

import android.content.Context

data class UserSession(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val currentSpaceId: String? = null,
    val spaceName: String? = null,
    val spaceCode: String? = null,
    val spaceStatus: String? = null,
    val partnerUsername: String? = null,
    val partnerAvatarUrl: String? = null,
    val isSpaceOwner: Boolean = false
)

class SessionSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "yanye_session_settings",
        Context.MODE_PRIVATE
    )

    fun loadSession(): UserSession? {
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val username = preferences.getString(KEY_USERNAME, null) ?: return null
        return UserSession(
            userId = userId,
            username = username,
            avatarUrl = preferences.getString(KEY_AVATAR_URL, null).nullIfLiteralNull(),
            currentSpaceId = preferences.getString(KEY_CURRENT_SPACE_ID, null).nullIfLiteralNull(),
            spaceName = preferences.getString(KEY_SPACE_NAME, null).nullIfLiteralNull(),
            spaceCode = preferences.getString(KEY_SPACE_CODE, null).nullIfLiteralNull(),
            spaceStatus = preferences.getString(KEY_SPACE_STATUS, null).nullIfLiteralNull(),
            partnerUsername = preferences.getString(KEY_PARTNER_USERNAME, null).nullIfLiteralNull(),
            partnerAvatarUrl = preferences.getString(KEY_PARTNER_AVATAR_URL, null).nullIfLiteralNull(),
            isSpaceOwner = preferences.getBoolean(KEY_IS_SPACE_OWNER, false)
        )
    }

    fun saveSession(session: UserSession) {
        preferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_AVATAR_URL, session.avatarUrl)
            .putString(KEY_CURRENT_SPACE_ID, session.currentSpaceId)
            .putString(KEY_SPACE_NAME, session.spaceName)
            .putString(KEY_SPACE_CODE, session.spaceCode)
            .putString(KEY_SPACE_STATUS, session.spaceStatus)
            .putString(KEY_PARTNER_USERNAME, session.partnerUsername)
            .putString(KEY_PARTNER_AVATAR_URL, session.partnerAvatarUrl)
            .putBoolean(KEY_IS_SPACE_OWNER, session.isSpaceOwner)
            .apply()
    }

    fun clearSession() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_CURRENT_SPACE_ID)
            .remove(KEY_SPACE_NAME)
            .remove(KEY_SPACE_CODE)
            .remove(KEY_SPACE_STATUS)
            .remove(KEY_PARTNER_USERNAME)
            .remove(KEY_PARTNER_AVATAR_URL)
            .remove(KEY_IS_SPACE_OWNER)
            .apply()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_CURRENT_SPACE_ID = "current_space_id"
        const val KEY_SPACE_NAME = "space_name"
        const val KEY_SPACE_CODE = "space_code"
        const val KEY_SPACE_STATUS = "space_status"
        const val KEY_PARTNER_USERNAME = "partner_username"
        const val KEY_PARTNER_AVATAR_URL = "partner_avatar_url"
        const val KEY_IS_SPACE_OWNER = "is_space_owner"
    }
}

private fun String?.nullIfLiteralNull(): String? =
    this?.takeUnless { it.isBlank() || it == "null" }
