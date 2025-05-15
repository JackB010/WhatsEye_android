package com.example.whatseye.dataType.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.whatseye.dataType.data.RecordingData
import java.sql.SQLException

class RecordingDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "recordings.db"
        private const val DATABASE_VERSION = 1

        // Table and columns
        const val TABLE_NAME = "recordings"
        const val COLUMN_ID = "id"
        const val COLUMN_RECORDING_TYPE = "recording_type"
        const val COLUMN_RECORD_FILE = "record_file"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECORDING_TYPE TEXT NOT NULL,
                $COLUMN_RECORD_FILE TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent()
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades if needed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Insert a new recording
    fun insertRecording(record: RecordingData): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_RECORDING_TYPE, record.recordingType)
            put(COLUMN_RECORD_FILE, record.recordFile)
            put(COLUMN_TIMESTAMP, record.timestamp)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // Retrieve all recordings
    fun getAllRecordings(): List<RecordingData> {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null, // all columns
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC" // order by timestamp descending
        )

        val recordings = mutableListOf<RecordingData>()
        with(cursor) {
            while (moveToNext()) {
                val record = RecordingData(
                    recordingType = getString(getColumnIndexOrThrow(COLUMN_RECORDING_TYPE)),
                    recordFile = getString(getColumnIndexOrThrow(COLUMN_RECORD_FILE)),
                    timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                )
                recordings.add(record)
            }
        }
        cursor.close()
        return recordings
    }
    fun deleteRecording(recordFilePath: String): Boolean {

        val db = writableDatabase
        return try {
            val whereClause = "$COLUMN_RECORD_FILE = ?"
            val whereArgs = arrayOf(recordFilePath)
            // Delete the entire row from the 'recordings' table
            val deletedRows = db.delete(TABLE_NAME, whereClause, whereArgs)
            deletedRows > 0
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }
}