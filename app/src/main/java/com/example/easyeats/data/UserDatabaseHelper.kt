package com.example.easyeats.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // ✅ Keep a single instance of the writable database
    private var db: SQLiteDatabase? = null

    override fun onCreate(db: SQLiteDatabase) {
        // Users table
        val createUserTable = """
            CREATE TABLE $TABLE_USER (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT
            )
        """.trimIndent()
        db.execSQL(createUserTable)

        // Favourite Restaurants table
        val createFavoritesTable = """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_FAV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID_FK INTEGER,
                $COLUMN_RESTAURANT TEXT,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_RATING REAL,
                $COLUMN_REVIEW TEXT,
                FOREIGN KEY($COLUMN_USER_ID_FK) REFERENCES $TABLE_USER($COLUMN_USER_ID)
            )
        """.trimIndent()
        db.execSQL(createFavoritesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        onCreate(db)
    }

    // ===== User functions =====
    fun insertUser(email: String, password: String): Boolean {
        val writableDb = getWritableDatabaseInstance()
        val values = ContentValues().apply {
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
        }
        val result = writableDb.insert(TABLE_USER, null, values)
        // ❌ Removed db.close()
        return result != -1L
    }

    fun checkUser(email: String, password: String): Boolean {
        val readableDb = getWritableDatabaseInstance() // use same DB instance
        val cursor = readableDb.rawQuery(
            "SELECT * FROM $TABLE_USER WHERE $COLUMN_EMAIL=? AND $COLUMN_PASSWORD=?",
            arrayOf(email, password)
        )
        val exists = cursor.count > 0
        cursor.close()
        // ❌ Removed db.close()
        return exists
    }

    // ===== Favourite Restaurants functions =====
    fun addFavorite(
        userId: Int,
        restaurant: String,
        address: String,
        rating: Float,
        review: String
    ): Boolean {
        val writableDb = getWritableDatabaseInstance()
        val values = ContentValues().apply {
            put(COLUMN_USER_ID_FK, userId)
            put(COLUMN_RESTAURANT, restaurant)
            put(COLUMN_ADDRESS, address)
            put(COLUMN_RATING, rating)
            put(COLUMN_REVIEW, review)
        }
        val result = writableDb.insert(TABLE_FAVORITES, null, values)
        // ❌ Removed db.close()
        return result != -1L
    }

    fun getFavorites(userId: Int): List<Map<String, Any>> {
        val readableDb = getWritableDatabaseInstance()
        val cursor = readableDb.rawQuery(
            "SELECT * FROM $TABLE_FAVORITES WHERE $COLUMN_USER_ID_FK=?",
            arrayOf(userId.toString())
        )
        val list = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            val item = mapOf(
                "restaurant" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESTAURANT)),
                "address" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                "rating" to cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RATING)),
                "review" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REVIEW))
            )
            list.add(item)
        }
        cursor.close()
        // ❌ Removed db.close()
        return list
    }

    // ✅ New: Single writable DB instance getter
    private fun getWritableDatabaseInstance(): SQLiteDatabase {
        if (db == null || !db!!.isOpen) {
            db = writableDatabase
        }
        return db!!
    }

    // ✅ Optional: call this in onDestroy() of MainActivity if you want to close DB
    fun closeDatabase() {
        db?.close()
        db = null
    }

    companion object {
        private const val DATABASE_NAME = "easyeats.db"
        private const val DATABASE_VERSION = 1

        // Users table
        const val TABLE_USER = "user"
        const val COLUMN_USER_ID = "userId"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"

        // Favourites table
        const val TABLE_FAVORITES = "favorites"
        const val COLUMN_FAV_ID = "favId"
        const val COLUMN_USER_ID_FK = "userId"
        const val COLUMN_RESTAURANT = "restaurant"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_RATING = "rating"
        const val COLUMN_REVIEW = "review"
    }
}
