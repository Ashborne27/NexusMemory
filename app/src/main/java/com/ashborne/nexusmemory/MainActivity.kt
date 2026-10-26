package com.ashborne.nexusmemory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
import androidx.work.*
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.data.MemoryEntity
import com.ashborne.nexusmemory.security.BiometricHelper
import com.ashborne.nexusmemory.security.VoiceRecorderManager
import com.ashborne.nexusmemory.sync.WebDavManager
import com.ashborne.nexusmemory.ui.AudioWaveformVisualizer
import com.ashborne.nexusmemory.ui.CameraQrScanner
import com.ashborne.nexusmemory.ui.QrCodeGenerator
import com.ashborne.nexusmemory.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var activeTempAudioFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = MemoryDatabase.getDatabase(applicationContext)
        val dao = database.memoryDao()
        val backupManager = BackupManager(applicationContext, dao)
        val voiceRecorder = VoiceRecorderManager(applicationContext)
        val prefs = getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)

        scheduleBackgroundSync(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            val memoriesFlow by dao.getAllMemories().collectAsState(initial = emptyList())
            var displayedMemories by remember { mutableStateOf<List<MemoryEntity>>(emptyList()) }
            var searchQuery by remember { mutableStateOf("") }
            var newThoughtText by remember { mutableStateOf("") }
            var isProtected by remember { mutableStateOf(false) }
            var isRecording by remember { mutableStateOf(false) }
            var playingAudioPath by remember { mutableStateOf<String?>(null) }
            var showScanner by remember { mutableStateOf(false) }
            var showWebDavDialog by remember { mutableStateOf(false) }
            var selectedMemoryForQr by remember { mutableStateOf<MemoryEntity?>(null) }
            var unlockedMemoryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
            var statusMessage by remember { mutableStateOf("NexusMemory v$${BuildConfig.VERSION_NAME}") }

            var webDavUrl by remember { mutableStateOf(prefs.getString("webdav_url", "https://example.com/webdav") ?: "") }
            var webDavUser by remember { mutableStateOf(prefs.getString("webdav_user", "user") ?: "") }
            var webDavPass by remember { mutableStateOf(prefs.getString("webdav_pass", "password") ?: "") }

            val audioPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    val file = voiceRecorder.startRecording()
                    if (file != null) {
                        isRecording = true
                        statusMessage = "Enregistrement vocal en cours..."
                    } else {
                        statusMessage = "Échec du démarrage du micro"
                    }
                } else {
                    statusMessage = "Permission microphone refusée par l'utilisateur."
                }
            }

            fun stopAudio() {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (_: Exception) {}
                mediaPlayer = null
                playingAudioPath = null
                activeTempAudioFile?.delete()
                activeTempAudioFile = null
                statusMessage = "Lecture arrêtée"
            }

            fun playAudio(path: String, isMemoryProtected: Boolean) {
                try {
                    stopAudio()
                    val targetFile = File(path)
                    val playFile = if (isMemoryProtected) {
                        val decrypted = VoiceRecorderManager.decryptAudioToCache(applicationContext, targetFile)
                        activeTempAudioFile = decrypted
                        decrypted
                    } else {
                        targetFile
                    }

                    if (playFile != null && playFile.exists()) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(playFile.absolutePath)
                            prepare()
                            start()
                            setOnCompletionListener {
                                stopAudio()
                                statusMessage = "Lecture terminée"
                            }
                        }
                        playingAudioPath = path
                        statusMessage = "Lecture de la note vocale..."
                    } else {
                        statusMessage = "Erreur : Fichier introuvable ou échec déchiffrement"
                    }
                } catch (e: Exception) {
                    statusMessage = "Erreur lecture : ${e.localizedMessage}"
                    stopAudio()
                }
            }

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NexusMemory v$${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    unlockedMemoryIds = emptySet()
                                    stopAudio()
                                    statusMessage = "Mémoires verrouillées !"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Verrouiller", style = MaterialTheme.typography.labelSmall)
                            }
                        }

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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showWebDavDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) { Text("Cloud WebDAV") }

                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) { Text("Scanner QR") }
                        }

                        Button(
                            onClick = {
                                if (!isRecording) {
                                    val checkPermission = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                    if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                                        val file = voiceRecorder.startRecording()
                                        if (file != null) {
                                            isRecording = true
                                            statusMessage = "Enregistrement vocal en cours..."
                                        } else {
                                            statusMessage = "Échec du démarrage du micro"
                                        }
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    val protectedState = isProtected
                                    val file = voiceRecorder.stopRecordingAndEncrypt(protectedState)
                                    isRecording = false
                                    if (file != null && file.exists()) {
                                        scope.launch(Dispatchers.IO) {
                                            dao.insertMemory(
                                                MemoryEntity(
                                                    title = if (protectedState) "Note Vocale Chiffrée" else "Mémoire Vocale",
                                                    content = "Audio: ${file.absolutePath}",
                                                    category = "Audio",
                                                    isProtected = protectedState
                                                )
                                            )
                                        }
                                        statusMessage = if (protectedState) "Note vocale chiffrée AES-256 !" else "Note vocale enregistrée !"
                                    } else {
                                        statusMessage = "Échec de la sauvegarde audio"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(if (isRecording) "🔴 Arrêter l'enregistrement vocal" else "🎙️ Enregistrer une note vocale")
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Recherche FTS rapide...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

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

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayedMemories) { memory ->
                                val isUnlocked = !memory.isProtected || unlockedMemoryIds.contains(memory.id)
                                val isAudio = memory.content.startsWith("Audio: ")
                                val audioPath = if (isAudio) memory.content.removePrefix("Audio: ") else null

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
                                            } else if (!isAudio) {
                                                selectedMemoryForQr = memory
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
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
                                        if (isUnlocked) {
                                            if (isAudio && audioPath != null) {
                                                val isPlayingThis = playingAudioPath == audioPath
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                if (isPlayingThis) stopAudio() else playAudio(audioPath, memory.isProtected)
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isPlayingThis) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                            )
                                                        ) {
                                                            Text(if (isPlayingThis) "⏹ Stop" else "▶ Écouter l'Audio")
                                                        }
                                                        Text(
                                                            text = if (isPlayingThis) "🔊 en lecture..." else if (memory.isProtected) "🔐 AES-256 Chiffré" else "📄 Fichier prêt",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    AudioWaveformVisualizer(isPlaying = isPlayingThis)
                                                }
                                            } else {
                                                Text(text = memory.content, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        } else {
                                            Text(
                                                text = "•••••••••••••••••••••••••••• (Appuyez pour déverrouiller)",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showWebDavDialog) {
                    AlertDialog(
                        onDismissRequest = { showWebDavDialog = false },
                        title = { Text("Cloud WebDAV") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = webDavUrl,
                                    onValueChange = { webDavUrl = it },
                                    label = { Text("URL Serveur WebDAV") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = webDavUser,
                                    onValueChange = { webDavUser = it },
                                    label = { Text("Utilisateur") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = webDavPass,
                                    onValueChange = { webDavPass = it },
                                    label = { Text("Mot de passe / Token") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        prefs.edit()
                                            .putString("webdav_url", webDavUrl)
                                            .putString("webdav_user", webDavUser)
                                            .putString("webdav_pass", webDavPass)
                                            .apply()

                                        showWebDavDialog = false
                                        statusMessage = "Téléversement WebDAV Chiffré..."
                                        scope.launch {
                                            val backupData = backupManager.exportToJson()
                                            val success = WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupData)
                                            statusMessage = if (success) "Sauvegarde WebDAV envoyée et chiffrée !" else "Échec téléversement WebDAV"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sauvegarder (Push Chiffré)")
                                }

                                Button(
                                    onClick = {
                                        prefs.edit()
                                            .putString("webdav_url", webDavUrl)
                                            .putString("webdav_user", webDavUser)
                                            .putString("webdav_pass", webDavPass)
                                            .apply()

                                        showWebDavDialog = false
                                        statusMessage = "Téléchargement WebDAV..."
                                        scope.launch {
                                            val downloadedData = WebDavManager.downloadFromWebDav(webDavUrl, webDavUser, webDavPass)
                                            if (downloadedData != null) {
                                                backupManager.importFromJson(downloadedData)
                                                statusMessage = "Restauration Cloud Chiffrée réussie !"
                                            } else {
                                                statusMessage = "Échec téléchargement WebDAV"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Restaurer (Pull Chiffré)")
                                }

                                TextButton(
                                    onClick = { showWebDavDialog = false },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Annuler")
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }

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
                                    Text("Erreur de génération QR Code")
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

    private fun scheduleBackgroundSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "NexusSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            activeTempAudioFile?.delete()
            activeTempAudioFile = null
        } catch (_: Exception) {}
    }
}
