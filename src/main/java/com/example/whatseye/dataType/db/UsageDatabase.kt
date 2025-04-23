package com.example.whatseye.dataType.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.whatseye.dataType.UsageData

class UsageDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "usage.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USAGE = "usage_data"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_USAGE_SECONDS = "usage_seconds"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USAGE (
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_HOUR INTEGER NOT NULL,
                $COLUMN_USAGE_SECONDS INTEGER,
                PRIMARY KEY ($COLUMN_DATE, $COLUMN_HOUR)
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USAGE")
        onCreate(db)
    }

    fun insertUsageData(date: String, hour: Int, usageSeconds: Long): Long {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_HOUR, hour)
            put(COLUMN_USAGE_SECONDS, usageSeconds)
        }

        val result = db.insertWithOnConflict(
            TABLE_USAGE,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        Log.d("UsageDatabase", "Inserted/Updated: date=$date, hour=$hour, usage_seconds=$usageSeconds")
        return result
    }

    @SuppressLint("Range")
    fun getUnsent(): List<UsageData> {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USAGE"
        val cursor = db.rawQuery(query, null)
        val unsentData = mutableListOf<UsageData>()

        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
                val hour = cursor.getInt(cursor.getColumnIndex(COLUMN_HOUR))
                val usageSeconds = cursor.getLong(cursor.getColumnIndex(COLUMN_USAGE_SECONDS))
                unsentData.add(UsageData(date, hour, usageSeconds))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return unsentData
    }

    fun deleteUsageData(date: String, hour: Int) {
        val db = writableDatabase
        db.delete(
            TABLE_USAGE,
            "$COLUMN_DATE = ? AND $COLUMN_HOUR = ?",
            arrayOf(date, hour.toString())
        )
        Log.d("UsageDatabase", "Deleted: date=$date, hour=$hour")
    }
}
