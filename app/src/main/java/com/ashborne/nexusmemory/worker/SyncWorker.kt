package com.ashborne.nexusmemory.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.Result
import com.ashborne.nexusmemory.backup.BackupManager
import com.ashborne.nexusmemory.data.MemoryDatabase
import com.ashborne.nexusmemory.sync.WebDavManager
import kotlinx.coroutines.runBlocking

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val prefs = ctx.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
            val webDavUrl = prefs.getString("webdav_url", null) ?: return Result.success()
            val webDavUser = prefs.getString("webdav_user", null) ?: return Result.success()
            val webDavPass = prefs.getString("webdav_pass", null) ?: return Result.success()

            val dao = MemoryDatabase.getDatabase(ctx).memoryDao()
            val backupManager = BackupManager(ctx, dao)

            val success = runBlocking {
                val backupJson = backupManager.exportToJson()
                WebDavManager.uploadToWebDav(webDavUrl, webDavUser, webDavPass, backupJson)
            }

            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
