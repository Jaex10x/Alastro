package com.example.decena

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProfileDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ProfileDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "profile"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_FIRST_NAME = "first_name"
        private const val COLUMN_LAST_NAME = "last_name"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_ABOUT = "about"
        private const val COLUMN_AVATAR_URI = "avatar_uri"

        private val dbLock = Any()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USERNAME TEXT NOT NULL, " +
                "$COLUMN_FIRST_NAME TEXT, " +
                "$COLUMN_LAST_NAME TEXT, " +
                "$COLUMN_PHONE TEXT, " +
                "$COLUMN_ABOUT TEXT, " +
                "$COLUMN_AVATAR_URI TEXT)"
        db.execSQL(createTable)

        // Insert default profile
        insertDefaultProfile(db)
    }

    private fun insertDefaultProfile(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, "User Name")
            put(COLUMN_FIRST_NAME, "First Name")
            put(COLUMN_LAST_NAME, "Last Name")
            put(COLUMN_PHONE, "Phone Number")
            put(COLUMN_ABOUT, "About You")
            put(COLUMN_AVATAR_URI, "")
        }
        db.insert(TABLE_NAME, null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getProfile(): Profile? {
        synchronized(dbLock) {
            val db = readableDatabase
            val query = "SELECT * FROM $TABLE_NAME LIMIT 1"
            val cursor = db.rawQuery(query, null)

            return try {
                if (cursor.moveToFirst()) {
                    Profile(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                        firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME)),
                        lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME)),
                        phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                        about = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ABOUT)),
                        avatarUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AVATAR_URI))
                    )
                } else {
                    null
                }
            } finally {
                cursor.close()
            }
        }
    }

    fun updateProfile(profile: Profile): Boolean {
        synchronized(dbLock) {
            val db = writableDatabase
            db.beginTransaction()
            return try {
                val values = ContentValues().apply {
                    put(COLUMN_USERNAME, profile.username)
                    put(COLUMN_FIRST_NAME, profile.firstName)
                    put(COLUMN_LAST_NAME, profile.lastName)
                    put(COLUMN_PHONE, profile.phone)
                    put(COLUMN_ABOUT, profile.about)
                    put(COLUMN_AVATAR_URI, profile.avatarUri)
                }
                val result = db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(profile.id.toString())) > 0
                db.setTransactionSuccessful()
                result
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                db.endTransaction()
            }
        }
    }
}