package com.example.easyeats.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.easyeats.Restaurant

class RestaurantDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "restaurants.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE restaurants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                rating REAL,
                photoUrl TEXT,
                address TEXT,
                latitude REAL,
                longitude REAL
            );
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS restaurants")
        onCreate(db)
    }

    fun insertRestaurant(restaurant: Restaurant) {
        val db = writableDatabase
        val values = ContentValues()

        values.put("name", restaurant.name)
        values.put("rating", restaurant.rating)
        values.put("photoUrl", restaurant.photoUrl)
        values.put("address", restaurant.address)
        values.put("latitude", restaurant.latitude)
        values.put("longitude", restaurant.longitude)

        db.insert("restaurants", null, values)
        db.close()
    }

    fun getAllRestaurants(): List<Restaurant> {
        val list = mutableListOf<Restaurant>()
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM restaurants", null)

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val rating = cursor.getDouble(cursor.getColumnIndexOrThrow("rating"))
                val photoUrl = cursor.getString(cursor.getColumnIndexOrThrow("photoUrl"))
                val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))

                list.add(
                    Restaurant(
                        name,
                        rating,
                        photoUrl,
                        address,
                        lat,
                        lng
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return list
    }
}
