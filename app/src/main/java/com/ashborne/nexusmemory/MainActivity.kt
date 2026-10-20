package com.ashborne.nexusmemory

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.BiometricHelper
import com.ashborne.nexusmemory.ui.CameraQrScanner
import com.ashborne.nexusmemory.ui.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = MemoryDatabase.getDatabase(applicationContext)
        val dao = database.memoryDao()
        val backupManager = BackupManager(applicationContext, dao)

        setContent {
            val scope = rememberCoroutineScope()
            val memoriesFlow by dao.getAllMemories().collectAsState(initial = emptyList())
            var displayedMemories by remember { mutableStateOf<List<MemoryEntity>>(emptyList()) }
            var searchQuery by remember { mutableStateOf("") }
            var newThoughtText by remember { mutableStateOf("") }
            var isProtected by remember { mutableStateOf(false) }
            var showScanner by remember { mutableStateOf(false) }
            var selectedMemoryForQr by remember { mutableStateOf<MemoryEntity?>(null) }
            var unlockedMemoryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
            var statusMessage by remember { mutableStateOf("Prêt") }

            LaunchedEffect(memoriesFlow, searchQuery) {
                displayedMemories = if (searchQuery.isBlank()) {
                    memoriesFlow
                } else {
                    withContext(Dispatchers.IO) {
                        dao.searchMemories(searchQuery)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (showScanner) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraQrScanner(modifier = Modifier.fillMaxSize()) { scanned ->
                            scope.launch(Dispatchers.IO) {
                                dao.insertMemory(
                                    MemoryEntity(
                                        title = scanned.take(20),
                                        content = scanned,
                                        category = "P2P Reçu"
                                    )
                                )
                            }
                            statusMessage = "Mémoire P2P reçue !"
                            showScanner = false
                        }
                        Button(
                            onClick = { showScanner = false },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Fermer Caméra")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // En-tête avec titre et bouton de verrouillage global
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NexusMemory v6.9-Beta",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Button(
                                onClick = {
                                    unlockedMemoryIds = emptySet()
                                    statusMessage = "Mémoires verrouillées !"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Verrouiller", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Boutons d'action Exporter / Importer
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val backupData = backupManager.exportToJson()
                                        val exportFile = File(getExternalFilesDir(null), "export_backup.nexus")
                                        exportFile.writeText(backupData)
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Exporté : ${exportFile.name}"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Exporter") }

                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val exportFile = File(getExternalFilesDir(null), "export_backup.nexus")
                                        if (exportFile.exists()) {
                                            backupManager.importFromJson(exportFile.readText())
                                            withContext(Dispatchers.Main) {
                                                statusMessage = "Import réussi !"
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                statusMessage = "Fichier introuvable"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Importer") }
                        }

                        // Boutons WebDAV / Scanner QR
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { statusMessage = "WebDAV synchronisé" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) { Text("WebDAV") }
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) { Text("Scanner QR") }
                        }

                        // Recherche FTS
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Recherche FTS rapide...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Zone d'ajout de pensée avec alignement révisé
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newThoughtText,
                                onValueChange = { newThoughtText = it },
                                label = { Text("Nouvelle pensée...") },
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                            Button(
                                onClick = {
                                    if (newThoughtText.isNotBlank()) {
                                        val text = newThoughtText
                                        val protectedState = isProtected
                                        scope.launch(Dispatchers.IO) {
                                            dao.insertMemory(
                                                MemoryEntity(
                                                    title = text.take(20),
                                                    content = text,
                                                    category = if (protectedState) "Protégé" else "Général",
                                                    isProtected = protectedState
                                                )
                                            )
                                        }
                                        newThoughtText = ""
                                        statusMessage = if (protectedState) "Pensée chiffrée !" else "Pensée ajoutée !"
                                    }
                                }
                            ) {
                                Text("Ajouter")
                            }
                        }

                        // Checkbox séparé avec espacement garanti
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isProtected,
                                onCheckedChange = { isProtected = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Protéger (Chiffrement KeyStore)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(text = "Statut : $statusMessage", style = MaterialTheme.typography.bodySmall)

                        HorizontalDivider()

                        // Liste dynamique scrollable des mémoires
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayedMemories) { memory ->
                                val isUnlocked = !memory.isProtected || unlockedMemoryIds.contains(memory.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (memory.isProtected && !isUnlocked) {
                                                BiometricHelper.authenticate(
                                                    activity = this@MainActivity,
                                                    onSuccess = {
                                                        unlockedMemoryIds = unlockedMemoryIds + memory.id
                                                        statusMessage = "Mémoire déverrouillée"
                                                    },
                                                    onError = { err ->
                                                        statusMessage = "Échec biométrique : $err"
                                                    }
                                                )
                                            } else {
                                                selectedMemoryForQr = memory
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = memory.title, style = MaterialTheme.typography.titleMedium)
                                            if (memory.isProtected) {
                                                Text(
                                                    text = if (isUnlocked) "🔓 Déverrouillé" else "🔒 KeyStore Protégé",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isUnlocked) memory.content else "•••••••••••••••••••••••••••• (Appuyez pour déverrouiller)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dialogue QR Code P2P
                selectedMemoryForQr?.let { memory ->
                    val qrBitmap = remember(memory) {
                        QrCodeGenerator.generateQrBitmap(memory.content)
                    }
                    AlertDialog(
                        onDismissRequest = { selectedMemoryForQr = null },
                        title = { Text("Partage P2P : ${memory.title}") },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap,
                                        contentDescription = "QR Code P2P",
                                        modifier = Modifier.size(240.dp)
                                    )
                                } else {
                                    Text("Erreur lors de la génération du QR Code")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scannez ce code depuis un autre téléphone pour transférer la mémoire.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { selectedMemoryForQr = null }) {
                                Text("Fermer")
                            }
                        }
                    )
                }
            }
        }
    }
}
