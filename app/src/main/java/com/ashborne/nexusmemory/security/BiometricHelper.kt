package com.ashborne.nexusmemory.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onSuccess()
    }
}
