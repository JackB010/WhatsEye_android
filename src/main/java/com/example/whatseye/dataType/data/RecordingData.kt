package com.example.whatseye.dataType.data

data class RecordingData(
    val recordingType: String,
    val recordFile: String,
    val timestamp: Long // or convert to Long (milliseconds)
)