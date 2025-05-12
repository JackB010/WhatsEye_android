package com.example.whatseye.dataType.data

data class ScheduleData (
    val id: Int,
    val name: String,
    val startTime: String,
    val endTime: String,
    val startDate: String,
    val endDate: String,
    val days: List<Int>
)