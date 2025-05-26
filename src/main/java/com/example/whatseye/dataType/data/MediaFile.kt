package com.example.whatseye.dataType.data
import android.net.Uri


data class MediaFile(
    val uri: Uri,
    val name: String,
    val type: MediaType,
    val size: Long,
    val lastModified: Long
)