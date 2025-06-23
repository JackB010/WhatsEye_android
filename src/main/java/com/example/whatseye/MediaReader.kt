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
     * Supports single and multi-account WhatsApp setups. Uses File API instead of MediaStore.
     * @return List of [MediaFile] objects, or empty list if access fails.
     */
    fun getAllMediaFiles(): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val maxFileSize = 10 * 1024 * 1024 // 10 MB in bytes

        // Define base paths for WhatsApp media (legacy and scoped storage)
        val basePaths = listOf(
            "/storage/emulated/0/WhatsApp/Media/", // Legacy path (single account)
            "/storage/emulated/0/WhatsApp/accounts/", // Legacy path (multi-account)
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/", // Scoped storage (single account)
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/accounts/" // Scoped storage (multi-account)
        )

        basePaths.forEach { basePath ->
            val baseDir = File(basePath)
            if (!baseDir.exists() || !baseDir.isDirectory) {
                Log.w("MediaReader", "Directory does not exist or is not a directory: $basePath")
                return@forEach
            }

            // For multi-account paths, find account-specific subdirectories
            val accountDirs = if (basePath.endsWith("accounts/")) {
                baseDir.listFiles { file -> file.isDirectory }?.toList() ?: emptyList()
            } else {
                listOf(baseDir) // Single account, use the base directory directly
            }

            accountDirs.forEach { accountDir ->
                // Find Media directory within each account folder
                val mediaDir = if (basePath.endsWith("accounts/")) {
                    File(accountDir, "Media")
                } else {
                    accountDir // Already pointing to Media directory
                }

                if (!mediaDir.exists() || !mediaDir.isDirectory) {
                    Log.w("MediaReader", "Media directory does not exist: ${mediaDir.absolutePath}")
                    return@forEach
                }

                // Recursively find all files in the Media directory
                mediaDir.walkTopDown()
                    .filter { it.isFile } // Only process files, not directories
                    .filter { it.length() <= maxFileSize } // Filter files <= 10 MB
                    .forEach { file ->
                        // Ensure the file is in a WhatsApp-related directory
                        if (!file.absolutePath.contains("WhatsApp", ignoreCase = true)) {
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
                        )
                    }
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
}