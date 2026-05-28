package com.yanye.home.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AutoSyncLifecycleEffect(
    onEnterSync: () -> Unit,
    onFlushSync: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEnterSync by rememberUpdatedState(onEnterSync)
    val currentOnFlushSync by rememberUpdatedState(onFlushSync)

    DisposableEffect(lifecycleOwner) {
        currentOnEnterSync()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> currentOnEnterSync()
                Lifecycle.Event.ON_STOP -> currentOnFlushSync()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            currentOnFlushSync()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
