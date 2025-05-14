package com.example.whatseye.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import com.example.whatseye.utils.uploadRecord
import java.io.File
import kotlin.properties.Delegates

class ScreenRecorderService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var windowManager: WindowManager? = null
    private var metrics: DisplayMetrics? = null
    private var width by Delegates.notNull<Int>()
    private var height by Delegates.notNull<Int>()
    private var outputPath: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        metrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val contextDisplay = windowManager?.defaultDisplay
            contextDisplay?.let {
                val bounds = windowManager!!.currentWindowMetrics.bounds
                metrics!!.widthPixels = bounds.width()
                metrics!!.heightPixels = bounds.height()
                metrics!!.densityDpi = resources.displayMetrics.densityDpi
                width = bounds.width()
                height = bounds.height()
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager!!.defaultDisplay?.getMetrics(metrics)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground before projection
        startForeground()
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                startRecording(resultCode, resultData)
            }
            ACTION_STOP -> {
                stopRecording()
                uploadRecord(this, "video", outputPath)

            }
        }
        return START_STICKY
    }
    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            stopRecording()
        }
    }
    private fun startRecording(resultCode: Int, data: Intent?) {
        val mediaDir = getValidMediaDirectory() ?: return
        setupMediaRecorder(mediaDir)



        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)

        // ✅ Register the callback BEFORE starting the capture
        mediaProjection?.registerCallback(mediaProjectionCallback, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecorder",
            metrics!!.widthPixels,
            metrics!!.heightPixels,
            metrics!!.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )

        mediaRecorder?.start()
    }

    @SuppressLint("NewApi")
    private fun setupMediaRecorder(mediaDir: File) {
        outputPath = File(mediaDir, "recording_${System.currentTimeMillis()}.mp4").absolutePath

        mediaRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputPath)
            setVideoEncoder(MediaRecorder.VideoEncoder.HEVC) // Or HEVC for better compression
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(5 * 100000) // Lower bitrate for smaller size
            setVideoFrameRate(5) // Lower frame rate
            setAudioSamplingRate(16000) // Lower audio sampling rate
            setAudioEncodingBitRate(32000) // Lower audio bitrate
            setVideoSize(width, height) // Lower resolution
            setMaxFileSize(50 * 1024 * 1024) // Optional: Limit file size to 50MB
            prepare()
        }
    }

    @SuppressLint("InlinedApi")
    private fun startForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        mediaRecorder?.reset()
        virtualDisplay?.release()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        stopForeground(true)
        stopSelf()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private fun getValidMediaDirectory(): File? {
        return externalMediaDirs.firstOrNull { it != null && it.exists() && it.canWrite() }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val NOTIFICATION_ID = 123
    }
}
