package com.ashborne.nexusmemory.worker

import android.content.Context
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NexusSyncManager {
    suspend fun executeSync(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
                val webDavUrl = prefs.getString("webdav_url", null) ?: return@withContext false
                val webDavUser = prefs.getString("webdav_user", null) ?: return@withContext false
                val webDavPass = prefs.getString("webdav_pass", null) ?: return@withContext false

                val dao = MemoryDatabase.getDatabase(context).memoryDao()
                val backupManager = BackupManager(context, dao)

                val backupJson = backupManager.exportToJson()
                WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
