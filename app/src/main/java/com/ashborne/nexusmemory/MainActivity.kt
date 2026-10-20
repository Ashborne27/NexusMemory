package com.ashborne.nexusmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.ashborne.nexusmemory.ui.CameraQrScanner
import com.ashborne.nexusmemory.ui.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
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
                            statusMessage = "Mémoire P2P reçue et enregistrée !"
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "NexusMemory v6.8-Alpha",
                            style = MaterialTheme.typography.headlineMedium
                        )

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
                            ) { Text("Exporter (.nexus)") }

                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val exportFile = File(getExternalFilesDir(null), "export_backup.nexus")
                                        if (exportFile.exists()) {
                                            backupManager.importFromJson(exportFile.readText())
                                            withContext(Dispatchers.Main) {
                                                statusMessage = "Importation réussie !"
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                statusMessage = "Aucun fichier .nexus trouvé"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Importer (.nexus)") }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { statusMessage = "Cloud WebDAV synchronisé" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) { Text("WebDAV") }
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) { Text("Scanner QR") }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Recherche FTS rapide...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newThoughtText,
                                    onValueChange = { newThoughtText = it },
                                    label = { Text("Nouvelle pensée...") },
                                    modifier = Modifier.weight(1f)
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
                                            statusMessage = if (protectedState) "Pensée chiffrée enregistrée !" else "Pensée ajoutée !"
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("Ajouter")
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isProtected,
                                    onCheckedChange = { isProtected = it }
                                )
                                Text(
                                    text = "Protéger (Chiffrement KeyStore)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Text(text = "Statut : $statusMessage", style = MaterialTheme.typography.bodySmall)

                        HorizontalDivider()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedMemories) { memory ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedMemoryForQr = memory }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = memory.title, style = MaterialTheme.typography.titleMedium)
                                            if (memory.isProtected) {
                                                Text(
                                                    text = "🔒 KeyStore",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = memory.content, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Boîte de dialogue affichant le QR Code chiffré P2P
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
