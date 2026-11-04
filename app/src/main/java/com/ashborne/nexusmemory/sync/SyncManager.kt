package com.ashborne.nexusmemory.sync

import android.content.Context
import com.ashborne.nexusmemory.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SyncManager {

    /**
     * Exécute l'export chiffré et l'envoi vers le serveur WebDAV
     */
    suspend fun performBackup(
        context: Context,
        serverUrl: String,
        user: String,
        token: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("nexus_memory.db")
            if (!dbFile.exists()) return@withContext false

            val encryptedFile = File(context.cacheDir, "nexus_memory_v10.enc")
            
            // 1. Chiffrement de bout en bout
            CryptoManager.encryptFile(dbFile, encryptedFile)

            // 2. Transfert sécurisé via le client WebDAV
            val client = WebDavClient(serverUrl, user, token)
            val success = client.uploadFile(encryptedFile, "NexusMemory/backup_v10.enc")

            // Nettoyage du cache local
            if (encryptedFile.exists()) encryptedFile.delete()

            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Télécharge le fichier chiffré distant et le déchiffre pour restaurer les données
     */
    suspend fun performRestore(
        context: Context,
        serverUrl: String,
        user: String,
        token: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(context.cacheDir, "downloaded_backup.enc")
            val client = WebDavClient(serverUrl, user, token)

            // 1. Téléchargement depuis le cloud
            val downloaded = client.downloadFile("NexusMemory/backup_v10.enc", encryptedFile)
            if (!downloaded || !encryptedFile.exists()) return@withContext false

            val dbFile = context.getDatabasePath("nexus_memory.db")
            
            // 2. Déchiffrement sécurisé
            CryptoManager.decryptFile(encryptedFile, dbFile)

            // Nettoyage
            if (encryptedFile.exists()) encryptedFile.delete()

            true
        } catch (e: Exception) {
            false
        }
    }
}
