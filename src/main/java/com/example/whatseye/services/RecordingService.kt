package com.example.whatseye.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import com.example.whatseye.utils.uploadRecord
import java.io.File
import java.util.concurrent.Executors


class RecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var outputPath: String = ""

    @Volatile private var isRecording = false

    private val executor = Executors.newSingleThreadExecutor()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRecording) {
                    executor.execute { startRecording() }
                }
            }
            ACTION_STOP -> {
                if (isRecording) {
                    executor.execute {
                        stopRecording()
                        uploadRecord(this, "voice", outputPath)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }



    private fun startRecording() {
        try {
            val mediaDir = getValidMediaDirectory() ?: run {
                Log.e(TAG, "No valid media directory found")
                return
            }

            mediaRecorder = createMediaRecorder(mediaDir).apply {
                prepare()
                start()
            }

            isRecording = true
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

        } catch (e: Exception) {
            handleRecorderError("Failed to start recording", e)
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.e(TAG, "RuntimeException during stop, file may be invalid", e)
                    File(outputPath).delete() // Clean up broken file
                } finally {
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during stop", e)
        } finally {
            cleanupResources()
            stopForeground(true)
            stopSelf()
        }
    }

    private fun getValidMediaDirectory(): File? {
        return externalMediaDirs.firstOrNull { it != null && it.exists() && it.canWrite() }
    }

    private fun createMediaRecorder(mediaDir: File): MediaRecorder {
        outputPath = File(mediaDir, "recording_${System.currentTimeMillis()}.m4a").absolutePath

        return MediaRecorder().apply {
            // You might want to switch this depending on the source you need
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputPath)
            setAudioChannels(1)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
        }
    }

    private fun handleRecorderError(message: String, e: Exception) {
        Log.e(TAG, "$message: ${e.message}", e)
        cleanupResources()
        stopForeground(true)
        stopSelf()
    }

    private fun cleanupResources() {
        mediaRecorder = null
        isRecording = false
    }

    private fun buildNotification(): Notification {
        val channelId = "recording_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                this,
                channelId,
                "Recording Service"
            )
        }

        return createNotification(
            context = this,
            channelId,
            title = "Recording Service",
            "Recording in progress..."
        )
    }

    override fun onDestroy() {
        if (isRecording) {
            stopRecording()
        }
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "RecordingService"
    }
}
