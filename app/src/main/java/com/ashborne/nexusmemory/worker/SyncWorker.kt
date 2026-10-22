package com.ashborne.nexusmemory.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
            val webDavUrl = prefs.getString("webdav_url", null) ?: return androidx.work.ListenableWorker.Result.success()
            val webDavUser = prefs.getString("webdav_user", null) ?: return androidx.work.ListenableWorker.Result.success()
            val webDavPass = prefs.getString("webdav_pass", null) ?: return androidx.work.ListenableWorker.Result.success()

            val dao = MemoryDatabase.getDatabase(applicationContext).memoryDao()
            val backupManager = BackupManager(applicationContext, dao)

            val backupJson = backupManager.exportToJson()
            val success = WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)

            if (success) androidx.work.ListenableWorker.Result.success() else androidx.work.ListenableWorker.Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            androidx.work.ListenableWorker.Result.retry()
        }
    }
}
