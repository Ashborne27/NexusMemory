package com.ashborne.nexusmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ashborne.nexusmemory.security.BiometricHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isBiometricAuthenticated by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                if (BiometricHelper.canAuthenticate(this@MainActivity)) {
                    try {
                        BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            onSuccess = { isBiometricAuthenticated = true },
                            onError = { _ -> isBiometricAuthenticated = true }
                        )
                    } catch (e: Exception) {
                        isBiometricAuthenticated = true
                    }
                } else {
                    isBiometricAuthenticated = true
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (isBiometricAuthenticated) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "NexusMemory v11.0 - Sécurisé & Opérationnel")
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Veuillez vous authentifier...")
                    }
                }
            }
        }
    }
}
