package com.ashborne.nexusmemory

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ashborne.nexusmemory.data.AppDatabase
import com.ashborne.nexusmemory.data.MemoryDao
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.AutoLockManager
import com.ashborne.nexusmemory.security.BiometricAuthManager
import com.ashborne.nexusmemory.viewmodel.MemoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Suppress("UNCHECKED_CAST")
class MemoryViewModelFactory(private val dao: MemoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemoryViewModel::class.java)) {
            return MemoryViewModel(dao) as T
        }
        throw IllegalArgumentException("Classe ViewModel inconnue")
    }
}

class MainActivity : FragmentActivity() {

    private val viewModel: MemoryViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        MemoryViewModelFactory(db.memoryDao())
    }

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
                        Toast.makeText(this, "Sauvegarde .nexus v6.6 exportée", Toast.LENGTH_LONG).show()
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
                        onUnlock = {
                            if (BiometricAuthManager.canAuthenticate(this)) {
                                BiometricAuthManager.authenticate(
                                    activity = this,
                                    onSuccess = { AutoLockManager.unlockVault() },
                                    onError = { err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
                                )
                            } else {
                                AutoLockManager.unlockVault()
                            }
                        }
                    )
                } else {
                    MainVaultScreen(
                        viewModel = viewModel,
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
        Text("Coffre Verrouillé", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Garde biométrique & protection AES-256 active", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUnlock) {
            Text("Déverrouillage Biométrique")
        }
    }
}

@Composable
fun MainVaultScreen(
    viewModel: MemoryViewModel,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onLockClick: () -> Unit
) {
    val memories by viewModel.allMemories.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var newMemoryText by remember { mutableStateOf("") }
    var isProtected by remember { mutableStateOf(false) }

    val categories = listOf("Tous", "Général", "Pro", "Perso")

    val filteredMemories = memories.filter { memory ->
        (selectedCategory == "Tous" || memory.category == selectedCategory) &&
        (searchQuery.isEmpty() || memory.text.contains(searchQuery, ignoreCase = true))
    }

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
            Text("NexusMemory v6.6", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Button(onClick = onLockClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Verrouiller")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onExportClick, modifier = Modifier.weight(1f)) {
                Text("Exporter (.nexus)")
            }
            OutlinedButton(onClick = onImportClick, modifier = Modifier.weight(1f)) {
                Text("Importer (.nexus)")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Recherche FTS4 rapide...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newMemoryText,
                onValueChange = { newMemoryText = it },
                label = { Text("Nouvelle pensée") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newMemoryText.isNotBlank()) {
                        val categoryToUse = if (selectedCategory == "Tous") "Général" else selectedCategory
                        viewModel.addMemory(newMemoryText, categoryToUse, isProtected)
                        newMemoryText = ""
                    }
                }
            ) {
                Text("Ajouter")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Checkbox(checked = isProtected, onCheckedChange = { isProtected = it })
            Text("Protéger (Chiffrement KeyStore)", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredMemories) { memory ->
                MemoryCard(memory = memory, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MemoryCard(memory: MemoryEntity, viewModel: MemoryViewModel) {
    var isDecrypted by remember { mutableStateOf(false) }
    var displayedText by remember { mutableStateOf(memory.text) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = memory.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(memory.timestamp))
                Text(text = dateStr, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (memory.isProtected && !isDecrypted) {
                Text(
                    text = "🔒 Contenu chiffré (AES-256)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = {
                    displayedText = viewModel.decryptContent(memory.text)
                    isDecrypted = true
                }) {
                    Text("Déchiffrer")
                }
            } else {
                Text(text = displayedText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
