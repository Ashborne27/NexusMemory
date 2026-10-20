package com.ashborne.nexusmemory.security

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class VoiceRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputAudioFile: File? = null

    fun startRecording(): File? {
        try {
            outputAudioFile = File(context.externalCacheDir, "nexus_voice_${System.currentTimeMillis()}.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputAudioFile?.absolutePath)
                prepare()
                start()
            }
            return outputAudioFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            outputAudioFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
