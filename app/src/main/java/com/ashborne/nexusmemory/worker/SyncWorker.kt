package com.ashborne.nexusmemory.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import kotlinx.coroutines.runBlocking
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): ListenableWorker.Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
            val webDavUrl = prefs.getString("webdav_url", null) ?: return ListenableWorker.Result.success()
            val webDavUser = prefs.getString("webdav_user", null) ?: return ListenableWorker.Result.success()
            val webDavPass = prefs.getString("webdav_pass", null) ?: return ListenableWorker.Result.success()

            val dao = MemoryDatabase.getDatabase(applicationContext).memoryDao()
            val backupManager = BackupManager(applicationContext, dao)

            val backupJson = backupManager.exportToJson()
            val success = runBlocking {
                WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)
            }

            if (success) ListenableWorker.Result.success() else ListenableWorker.Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            ListenableWorker.Result.retry()
        }
    }
}
