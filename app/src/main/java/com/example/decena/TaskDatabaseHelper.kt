package com.example.decena

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TasksDatabase.db"
        private const val DATABASE_VERSION = 4 // Increased version
        private const val TABLE_NAME = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_PRIORITY = "priority"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_IS_COMPLETED = "is_completed"

        private val dbLock = Any()
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

        // Add some sample data for testing
        addSampleData(db)
    }

    private fun addSampleData(db: SQLiteDatabase) {
        val calendar = Calendar.getInstance()
        val values = ContentValues().apply {
            put(COLUMN_TITLE, "Sample Task 1")
            put(COLUMN_DESCRIPTION, "This is a sample task")
            put(COLUMN_DATE, calendar.timeInMillis)
            put(COLUMN_TIME, "10:00 AM")
            put(COLUMN_PRIORITY, "Medium")
            put(COLUMN_CATEGORY, "General")
            put(COLUMN_IS_COMPLETED, 0)
        }
        db.insert(TABLE_NAME, null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addTask(task: Task): Long {
        synchronized(dbLock) {
            val db = writableDatabase
            db.beginTransaction()
            var id: Long = -1
            try {
                val values = ContentValues().apply {
                    put(COLUMN_TITLE, task.title)
                    put(COLUMN_DESCRIPTION, task.description)
                    put(COLUMN_DATE, task.date)
                    put(COLUMN_TIME, task.time)
                    put(COLUMN_PRIORITY, task.priority)
                    put(COLUMN_CATEGORY, task.category)
                    put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
                }
                id = db.insert(TABLE_NAME, null, values)
                db.setTransactionSuccessful()
                println("✅ Added task '${task.title}' with ID: $id")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
            return id
        }
    }

    fun getTasksForDateRange(startDate: Long, endDate: Long): List<Task> {
        synchronized(dbLock) {
            val tasks = mutableListOf<Task>()
            val db = readableDatabase

            val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_DATE >= ? AND $COLUMN_DATE <= ? ORDER BY $COLUMN_DATE ASC"
            val cursor = db.rawQuery(query, arrayOf(startDate.toString(), endDate.toString()))

            try {
                while (cursor.moveToNext()) {
                    val task = Task(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                        priority = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1
                    )
                    tasks.add(task)
                }
            } finally {
                cursor.close()
            }

            println("📊 Found ${tasks.size} tasks between ${Date(startDate)} and ${Date(endDate)}")
            return tasks
        }
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        synchronized(dbLock) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val values = ContentValues().apply {
                    put(COLUMN_IS_COMPLETED, if (isCompleted) 1 else 0)
                }
                db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
                db.setTransactionSuccessful()
                println("✅ Updated task $taskId completion to $isCompleted")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
        }
    }
    fun getTasksForDate(dateInMillis: Long): List<Task> {
        // Calculate start and end of the day
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

        // Use the date range method
        return getTasksForDateRange(startOfDay, endOfDay)
    }

    fun deleteTask(taskId: Int) {
        synchronized(dbLock) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
                db.setTransactionSuccessful()
                println("✅ Deleted task $taskId")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun getAllTasks(): List<Task> {
        synchronized(dbLock) {
            val tasks = mutableListOf<Task>()
            val db = readableDatabase
            val query = "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE DESC"
            val cursor = db.rawQuery(query, null)

            try {
                while (cursor.moveToNext()) {
                    val task = Task(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                        priority = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1
                    )
                    tasks.add(task)
                }
            } finally {
                cursor.close()
            }
            return tasks
        }
    }
}