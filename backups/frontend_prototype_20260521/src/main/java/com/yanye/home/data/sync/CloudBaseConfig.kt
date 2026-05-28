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
}
