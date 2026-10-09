package com.ashborne.nexusmemory

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ProcessLifecycleOwner
import com.ashborne.nexusmemory.security.AutoLockManager
import com.ashborne.nexusmemory.viewmodel.MemoryViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MemoryViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                val tempFile = File(cacheDir, "temp_export.nexus")
                viewModel.exportBackup(this, tempFile) { success ->
                    if (success) {
                        outputStream.write(tempFile.readBytes())
                        tempFile.delete()
                        Toast.makeText(this, "Exportation réussie", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Échec de l'exportation", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            contentResolver.openInputStream(targetUri)?.use { inputStream ->
                val tempFile = File(cacheDir, "temp_import.nexus")
                tempFile.writeBytes(inputStream.readBytes())
                viewModel.importBackup(this, tempFile) { result ->
                    tempFile.delete()
                    result.onSuccess { count ->
                        Toast.makeText(this, "$count pensées restaurées", Toast.LENGTH_LONG).show()
                    }.onFailure {
                        Toast.makeText(this, "Restauration échouée", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProcessLifecycleOwner.get().lifecycle.addObserver(AutoLockManager)

        setContent {
            val isLocked by AutoLockManager.isLocked.collectAsState()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (isLocked) {
                    LockedScreen(
                        onUnlock = { AutoLockManager.unlockVault() }
                    )
                } else {
                    MainScreen(
                        onExportClick = { exportLauncher.launch("NexusMemory-Vault.nexus") },
                        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
                        onLockClick = { AutoLockManager.lockVault() }
                    )
                }
            }
        }
    }
}

@Composable
fun LockedScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Coffre Verrouillé",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Protection passive activée.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUnlock) {
            Text("Déverrouiller")
        }
    }
}

@Composable
fun MainScreen(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onLockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NexusMemory v6.5",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onLockClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Verrouiller")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onExportClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Exporter (.nexus)")
            }
            Button(
                onClick = onImportClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Importer (.nexus)")
            }
        }
    }
}
