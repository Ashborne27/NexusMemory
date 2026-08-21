package com.nexus.memory

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class MemoryMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val INTERVAL = 5000L // Surveillance toutes les 5 secondes

    private val runnable = object : Runnable {
        override fun run() {
            monitorMemory()
            handler.postDelayed(this, INTERVAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(runnable)
        return START_STICKY // Assure la persistance après un redémarrage système
    }

    private fun monitorMemory() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        // Seuil critique : Si la mémoire disponible est inférieure à 15%
        if (mi.threshold > mi.availMem) {
            Log.w("NexusMemory", "ALERTE: Saturation RAM détectée. Activation du protocole de délestage.")
            performAggressiveCleanup(am)
        }
    }

    private fun performAggressiveCleanup(am: ActivityManager) {
        // Nettoyage agressif des processus secondaires
        // On cible les processus de background inutiles pour libérer la RAM
        val runningProcesses = am.runningAppProcesses
        runningProcesses?.forEach { process ->
            if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                // Envoi d'un signal de nettoyage aux processus non-critiques
                android.os.Process.killProcess(process.pid) 
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

