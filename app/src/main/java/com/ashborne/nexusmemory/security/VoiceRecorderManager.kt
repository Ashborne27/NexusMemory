package com.ashborne.nexusmemory.security

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

class VoiceRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var tempRawFile: File? = null

    fun startRecording(): File? {
        return try {
            tempRawFile = File(context.cacheDir, "temp_recording_raw.m4a")
            if (tempRawFile?.exists() == true) {
                tempRawFile?.delete()
            }

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(tempRawFile?.absolutePath)
                prepare()
                start()
            }
            tempRawFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stopRecordingAndEncrypt(isProtected: Boolean): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val raw = tempRawFile ?: return null
            if (!raw.exists()) return null

            val finalFile = File(context.getExternalFilesDir(null), "audio_note_${System.currentTimeMillis()}.enc")

            if (isProtected) {
                val cipher = KeyStoreHelper.getEncryptCipher()
                val iv = cipher.iv

                FileOutputStream(finalFile).use { fos ->
                    // Écriture explicite sans ambiguïté d'overload
                    fos.write(ByteArray(1) { iv.size.toByte() })
                    fos.write(iv)
                    CipherOutputStream(fos, cipher).use { cos ->
                        FileInputStream(raw).use { fis ->
                            fis.copyTo(cos)
                        }
                    }
                }
                raw.delete()
            } else {
                raw.copyTo(finalFile, overwrite = true)
                raw.delete()
            }

            finalFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        fun decryptAudioToCache(context: Context, encryptedFile: File): File? {
            return try {
                if (!encryptedFile.exists()) return null
                val tempCacheFile = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}.m4a")

                FileInputStream(encryptedFile).use { fis ->
                    val ivSize = fis.read()
                    if (ivSize == -1) return null
                    val iv = ByteArray(ivSize)
                    fis.read(iv)

                    val cipher = KeyStoreHelper.getDecryptCipher(iv)
                    CipherInputStream(fis, cipher).use { cis ->
                        FileOutputStream(tempCacheFile).use { fos ->
                            cis.copyTo(fos)
                        }
                    }
                }
                tempCacheFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
