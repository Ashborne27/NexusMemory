package com.ashborne.nexusmemory.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager
import kotlinx.coroutines.runBlocking

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            val ctx = applicationContext
            val prefs = ctx.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
            val webDavUrl = prefs.getString("webdav_url", null) ?: return androidx.work.ListenableWorker.Result.success()
            val webDavUser = prefs.getString("webdav_user", null) ?: return androidx.work.ListenableWorker.Result.success()
            val webDavPass = prefs.getString("webdav_pass", null) ?: return androidx.work.ListenableWorker.Result.success()

            val dao = MemoryDatabase.getDatabase(ctx).memoryDao()
            val backupManager = BackupManager(ctx, dao)

            val success = runBlocking {
                val backupJson = backupManager.exportToJson()
                WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)
            }

            if (success) androidx.work.ListenableWorker.Result.success() else androidx.work.ListenableWorker.Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            androidx.work.ListenableWorker.Result.retry()
        }
    }
}
