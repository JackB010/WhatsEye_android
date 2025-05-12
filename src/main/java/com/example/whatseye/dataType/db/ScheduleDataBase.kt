package com.example.whatseye.dataType.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.whatseye.dataType.data.ScheduleData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleDataBase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_DAY_TABLE)
        db.execSQL(CREATE_SCHEDULE_TABLE)
        db.execSQL(CREATE_SCHEDULE_DAY_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS schedule_day")
        db.execSQL("DROP TABLE IF EXISTS schedule")
        db.execSQL("DROP TABLE IF EXISTS day")
        onCreate(db)
    }

    // Insert a schedule and link it to given days
    fun insertSchedule(data: ScheduleData) {
        val db = writableDatabase

        // Insert the schedule row without 'isDeleted'
        db.execSQL(
            """
            INSERT OR REPLACE INTO schedule (
                id, name, start_time, end_time,
                start_date, end_date
            ) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent(), arrayOf(
                data.id, data.name, data.startTime, data.endTime,
                data.startDate, data.endDate
            )
        )

        // Sunday = 1, ..., Saturday = 7
        val dayNames = mapOf(
            1 to "Sunday", 2 to "Monday", 3 to "Tuesday",
            4 to "Wednesday", 5 to "Thursday",
            6 to "Friday", 7 to "Saturday"
        )

        // Insert days if they do not exist and link schedule to the day
        for (day in data.days) {
            val dayName = dayNames[day] ?: "Unknown"
            db.execSQL(
                "INSERT OR IGNORE INTO day (value, name) VALUES (?, ?)",
                arrayOf(day, dayName)
            )
            db.execSQL(
                "INSERT OR REPLACE INTO schedule_day (schedule_id, day_value) VALUES (?, ?)",
                arrayOf(data.id, day)
            )
        }
    }

    companion object {
        const val DATABASE_NAME = "schedule.db"
        const val DATABASE_VERSION = 1

        // Table to store days of the week (1 = Sunday to 7 = Saturday)
        private const val CREATE_DAY_TABLE = """
            CREATE TABLE day (
                value INTEGER PRIMARY KEY,
                name TEXT
            )
        """

        // Table to store schedule details
        private const val CREATE_SCHEDULE_TABLE = """
            CREATE TABLE schedule (
                id INTEGER PRIMARY KEY,
                name TEXT,
                start_time TEXT,
                end_time TEXT,
                start_date TEXT,
                end_date TEXT
            )
        """

        // Table to store links between schedule and days
        private const val CREATE_SCHEDULE_DAY_TABLE = """
            CREATE TABLE schedule_day (
                schedule_id INTEGER,
                day_value INTEGER,
                PRIMARY KEY (schedule_id, day_value),
                FOREIGN KEY(schedule_id) REFERENCES schedule(id),
                FOREIGN KEY(day_value) REFERENCES day(value)
            )
        """
    }

    fun getActiveSchedules(): List<ScheduleData> {
        val db = readableDatabase
        val scheduleList = mutableListOf<ScheduleData>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7

        val cursor: Cursor = db.rawQuery(
            """
        SELECT s.id, s.name, s.start_time, s.end_time, s.start_date, s.end_date
        FROM schedule s
        JOIN schedule_day sd ON s.id = sd.schedule_id
        WHERE sd.day_value = ?
    """.trimIndent(), arrayOf(currentDayOfWeek.toString())
        )

        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val startTime = cursor.getString(2)
            val endTime = cursor.getString(3)
            val startDate = cursor.getString(4)
            val endDate = cursor.getString(5)

            // Check if current date and time are within the range
            val isDateInRange = currentDate in startDate..endDate
            val isTimeInRange = currentTime in startTime..endTime
            if (isDateInRange && isTimeInRange) {
                // Retrieve days for this schedule
                val daysCursor = db.rawQuery(
                    "SELECT day_value FROM schedule_day WHERE schedule_id = ?",
                    arrayOf(id.toString())
                )
                val days = mutableListOf<Int>()
                while (daysCursor.moveToNext()) {
                    days.add(daysCursor.getInt(0))
                }
                daysCursor.close()

                val schedule =
                    ScheduleData(id, name, startTime, endTime, startDate, endDate, days)
                scheduleList.add(schedule)
            }
        }

        cursor.close()
        return scheduleList
    }

    fun getAllSchedules(): List<ScheduleData> {
        val db = readableDatabase
        val scheduleList = mutableListOf<ScheduleData>()

        // Query all schedules from the database without date/time range filtering
        val cursor: Cursor = db.rawQuery(
            """
        SELECT s.id, s.name, s.start_time, s.end_time, s.start_date, s.end_date
        FROM schedule s
    """.trimIndent(), null
        )

        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val startTime = cursor.getString(2)
            val endTime = cursor.getString(3)
            val startDate = cursor.getString(4)
            val endDate = cursor.getString(5)

            // Retrieve days for this schedule
            val daysCursor = db.rawQuery(
                "SELECT day_value FROM schedule_day WHERE schedule_id = ?",
                arrayOf(id.toString())
            )
            val days = mutableListOf<Int>()
            while (daysCursor.moveToNext()) {
                days.add(daysCursor.getInt(0))
            }
            daysCursor.close()

            val schedule = ScheduleData(id, name, startTime, endTime, startDate, endDate, days)
            scheduleList.add(schedule)
        }

        cursor.close()
        return scheduleList
    }


    fun deleteSchedule(id: Int) {
        val db = writableDatabase
        db.execSQL(
            "DELETE FROM schedule WHERE id=?",
            arrayOf(id)
        )

    }
}
