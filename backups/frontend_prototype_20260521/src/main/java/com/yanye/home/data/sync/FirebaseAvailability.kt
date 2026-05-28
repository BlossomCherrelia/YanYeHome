package com.yanye.home.data.sync

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseAvailability {
    fun isConfigured(context: Context): Boolean =
        runCatching {
            FirebaseApp.initializeApp(context.applicationContext)
            FirebaseApp.getApps(context.applicationContext).isNotEmpty()
        }.getOrDefault(false)
}
