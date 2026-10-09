package com.ashborne.nexusmemory.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AutoLockManager : DefaultLifecycleObserver {

    private const val LOCK_TIMEOUT_MS = 30_000L // 30 secondes
    private var backgroundTimestamp: Long = 0L

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    override fun onStop(owner: LifecycleOwner) {
        // L'application passe en arrière-plan
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        // L'application revient au premier plan
        if (backgroundTimestamp > 0 && (System.currentTimeMillis() - backgroundTimestamp) > LOCK_TIMEOUT_MS) {
            lockVault()
        }
    }

    fun unlockVault() {
        _isLocked.value = false
    }

    fun lockVault() {
        _isLocked.value = true
        backgroundTimestamp = 0L
    }
}
