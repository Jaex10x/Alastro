package com.example.decena

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TasksDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_PRIORITY = "priority"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_IS_COMPLETED = "is_completed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TITLE TEXT NOT NULL, " +
                "$COLUMN_DESCRIPTION TEXT, " +
                "$COLUMN_DATE INTEGER NOT NULL, " +
                "$COLUMN_TIME TEXT NOT NULL, " +
                "$COLUMN_PRIORITY TEXT NOT NULL, " +
                "$COLUMN_CATEGORY TEXT NOT NULL, " +
                "$COLUMN_IS_COMPLETED INTEGER DEFAULT 0)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addTask(task: Task): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_DATE, task.date)
            put(COLUMN_TIME, task.time)
            put(COLUMN_PRIORITY, task.priority)
            put(COLUMN_CATEGORY, task.category)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getTasksForDate(dateInMillis: Long): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase

        // Get start and end of the selected day
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_DATE BETWEEN ? AND ? ORDER BY $COLUMN_TIME ASC"
        val cursor = db.rawQuery(query, arrayOf(startOfDay.toString(), endOfDay.toString()))

        with(cursor) {
            while (moveToNext()) {
                val task = Task(
                    id = getInt(getColumnIndexOrThrow(COLUMN_ID)),
                    title = getString(getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = getString(getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    date = getLong(getColumnIndexOrThrow(COLUMN_DATE)),
                    time = getString(getColumnIndexOrThrow(COLUMN_TIME)),
                    priority = getString(getColumnIndexOrThrow(COLUMN_PRIORITY)),
                    category = getString(getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    isCompleted = getInt(getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1
                )
                tasks.add(task)
            }
            close()
        }
        return tasks
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_COMPLETED, if (isCompleted) 1 else 0)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
    }

    fun deleteTask(taskId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
    }
}