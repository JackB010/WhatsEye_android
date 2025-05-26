package com.example.whatseye

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.whatseye.dataType.data.MediaFile
import com.example.whatseye.dataType.data.MediaType
import java.io.File

class MediaReader(
    private val context: Context
) {
    /**
     * Retrieves a list of WhatsApp media files (images, audio including Opus, video) and documents
     * from external storage, with file size less than 10 MB, including Sent and Private subdirectories.
     * Uses File API instead of MediaStore.
     * @return List of [MediaFile] objects, or empty list if access fails.
     */
    fun getAllMediaFiles(): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val basePaths = listOf(
            "/sdcard/WhatsApp/Media/", // Legacy path (API < 30)
            "/sdcard/Android/media/com.whatsapp/WhatsApp/Media/" // Scoped storage path (API 30+)
        )

        basePaths.forEach { basePath ->
            val baseDir = File(basePath)
            if (!baseDir.exists() || !baseDir.isDirectory ) {
                Log.w("MediaReader", "Directory does not exist or is not a directory: $basePath")
                return@forEach
            }
            val whatsappFolders = getWhatsAppFolders(baseDir)
            whatsappFolders.forEach { folder ->
                Log.d("MediaReader", "Found WhatsApp folder: ${folder.absolutePath}")
                // You can walk through each folder if needed
                folder.walkTopDown().filter { it.isFile && it.length() < 10_000_000 }.forEach { file ->
                    // process file

                // Filter for WhatsApp subdirectories (e.g., WhatsApp Audio, WhatsApp Audio/Sent)
                if (!file.absolutePath.contains("WhatsApp")) {
                    Log.d("MediaReader", "Skipping non-WhatsApp file: ${file.absolutePath}")
                    return@forEach
                }

                val name = file.name

                val mimeType = getMimeType(file.extension.lowercase())
                if (mimeType == null) {
                    Log.d("MediaReader", "Skipping file with unrecognized extension: $name")
                    return@forEach
                }

                val mediaType = when {
                    mimeType.startsWith("image/") -> MediaType.IMAGE
                    mimeType.startsWith("audio/") || mimeType == "audio/ogg" -> MediaType.AUDIO
                    mimeType.startsWith("video/") -> MediaType.VIDEO
                    mimeType.startsWith("application/") || mimeType.startsWith("text/") -> MediaType.DOCUMENT
                    else -> {
                        Log.d("MediaReader", "Skipping file with unrecognized MIME type: $mimeType")
                        return@forEach
                    }
                }

                mediaFiles.add(
                    MediaFile(
                        uri = Uri.fromFile(file),
                        name = name,
                        type = mediaType,
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                )
                Log.d(
                    "MediaReader",
                    "Added file: name=$name, type=$mediaType, size=${file.length()}, modified=${file.lastModified()}, path=${file.absolutePath}"
                )            }
        }
        }

        Log.d("MediaReader", "Retrieved ${mediaFiles.size} WhatsApp files: ${mediaFiles.map { it.name }}")
        return mediaFiles.toList()
    }


    /**
     * Maps file extensions to MIME types for common WhatsApp media and document types.
     */
    private fun getMimeType(extension: String): String? {
        return when (extension) {
            "jpg", "jpeg", "png", "gif" -> "image/$extension"
            "mp3", "aac", "wav" -> "audio/$extension"
            "opus" -> "audio/ogg" // WhatsApp voice messages
            "mp4", "3gp" -> "video/$extension"
            "pdf", "doc", "docx", "txt" -> "application/$extension"
            else -> null
        }
    }
    private fun getWhatsAppFolders(baseDir: File): List<File> {
        return baseDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("WhatsApp", ignoreCase = true)
        }?.toList() ?: emptyList()
    }
}