package com.ashborne.nexusmemory

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ashborne.nexusmemory.data.AppDatabase
import com.ashborne.nexusmemory.data.MemoryDao
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.AutoLockManager
import com.ashborne.nexusmemory.security.BiometricAuthManager
import com.ashborne.nexusmemory.sync.QrUtils
import com.ashborne.nexusmemory.sync.SyncManager
import com.ashborne.nexusmemory.ui.QrScannerView
import com.ashborne.nexusmemory.viewmodel.MemoryViewModel
import kotlinx.coroutines.launch
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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission caméra requise pour le scan P2P", Toast.LENGTH_LONG).show()
        }
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
                        Toast.makeText(this, "Sauvegarde .nexus v6.8-Alpha exportée", Toast.LENGTH_LONG).show()
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

        // Demande de permission caméra au démarrage si absente
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

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
                        onLockClick = { AutoLockManager.lockVault() },
                        onSyncUp = { url, user, pass ->
                            lifecycleScope.launch {
                                val db = AppDatabase.getDatabase(applicationContext)
                                val success = SyncManager.syncUp(this@MainActivity, db.memoryDao(), url, user, pass)
                                val msg = if (success) "Synchronisation Cloud réussie !" else "Échec de la synchronisation WebDAV"
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        onSyncDown = { url, user, pass ->
                            lifecycleScope.launch {
                                val db = AppDatabase.getDatabase(applicationContext)
                                val res = SyncManager.syncDown(this@MainActivity, db.memoryDao(), url, user, pass)
                                res.onSuccess { count ->
                                    Toast.makeText(this@MainActivity, "$count pensées récupérées du Cloud", Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(this@MainActivity, "Échec de récupération Cloud", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
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
    onLockClick: () -> Unit,
    onSyncUp: (String, String, String) -> Unit,
    onSyncDown: (String, String, String) -> Unit
) {
    val memories by viewModel.allMemories.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var newMemoryText by remember { mutableStateOf("") }
    var isProtected by remember { mutableStateOf(false) }

    var showSyncDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val categories = listOf("Tous", "Général", "Pro", "Perso")

    val filteredMemories = memories.filter { memory ->
        (selectedCategory == "Tous" || memory.category == selectedCategory) &&
        (searchQuery.isEmpty() || memory.text.contains(searchQuery, ignoreCase = true))
    }

    if (showScannerDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            QrScannerView(
                onQrScanned = { scannedPayload ->
                    showScannerDialog = false
                    // Insertion directe de la pensée partagée par P2P
                    val categoryToUse = if (selectedCategory == "Tous") "Général" else selectedCategory
                    viewModel.addMemory(scannedPayload, categoryToUse, true)
                }
            )
            Button(
                onClick = { showScannerDialog = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text("Fermer le Scanner")
            }
        }
    } else {
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
                Text("NexusMemory v6.8-Alpha", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Button(onClick = onLockClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Verrouiller")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSyncDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Cloud WebDAV")
                }
                Button(
                    onClick = { showScannerDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Scanner QR (P2P)")
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

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Configuration Cloud WebDAV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("URL Serveur (ex: https://.../webdav)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Utilisateur") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe / Token") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onSyncUp(serverUrl, username, password)
                        showSyncDialog = false
                    }) {
                        Text("Envoyer (Push)")
                    }
                    TextButton(onClick = {
                        onSyncDown(serverUrl, username, password)
                        showSyncDialog = false
                    }) {
                        Text("Recevoir (Pull)")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun MemoryCard(memory: MemoryEntity, viewModel: MemoryViewModel) {
    var isDecrypted by remember { mutableStateOf(false) }
    var displayedText by remember { mutableStateOf(memory.text) }
    var showQrDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(memory.timestamp))
                    Text(text = dateStr, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showQrDialog = true },
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("QR Partage", style = MaterialTheme.typography.labelSmall)
                    }
                }
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

    if (showQrDialog) {
        val rawContent = if (memory.isProtected) memory.text else viewModel.encryptContent(memory.text)
        val qrBitmap = remember { QrUtils.generateQrBitmap("NEXUS_SHARE:$rawContent") }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Partage E2EE par QR-Code") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scannez ce code depuis un autre NexusMemory.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code de partage",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        Text("Erreur de génération du QR-code")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}
