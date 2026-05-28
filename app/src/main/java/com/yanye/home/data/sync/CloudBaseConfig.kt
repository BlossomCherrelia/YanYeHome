package com.yanye.home.data.sync

object CloudBaseConfig {
    const val ENV_ID = "yanyehome-d9grtwqrlc809509f"
    private const val BASE_URL =
        "https://yanyehome-d9grtwqrlc809509f-1434875599.ap-shanghai.app.tcloudbase.com"

    const val SYNC_ANNIVERSARIES_URL = "$BASE_URL/syncAnniversaries"

    const val SYNC_WISHES_URL = "$BASE_URL/syncWishes"

    const val SYNC_SCHEDULES_URL = "$BASE_URL/syncSchedules"

    const val SYNC_RESTAURANTS_URL = "$BASE_URL/syncRestaurants"

    const val SYNC_FOOTPRINTS_URL = "$BASE_URL/syncFootprints"

    const val SYNC_CARE_CYCLES_URL = "$BASE_URL/syncCareCycles"

    const val SYNC_MEMOS_URL = "$BASE_URL/syncMemos"

    const val SYNC_MEMORIES_URL = "$BASE_URL/syncMemories"

    const val UPLOAD_IMAGE_URL = "$BASE_URL/uploadImage"

    const val REGISTER_USER_URL = "$BASE_URL/registerUser"

    const val LOGIN_USER_URL = "$BASE_URL/loginUser"

    const val CREATE_COUPLE_SPACE_URL = "$BASE_URL/createCoupleSpace"

    const val CREATE_INVITE_CODE_URL = "$BASE_URL/createInviteCode"

    const val JOIN_COUPLE_SPACE_BY_INVITE_URL = "$BASE_URL/joinCoupleSpaceByInvite"

    const val GET_CURRENT_SESSION_PROFILE_URL = "$BASE_URL/getCurrentSessionProfile"

    const val UPDATE_SESSION_PROFILE_URL = "$BASE_URL/updateSessionProfile"
}
