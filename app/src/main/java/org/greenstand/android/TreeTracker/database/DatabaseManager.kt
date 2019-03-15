package org.greenstand.android.TreeTracker.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * Created by lei on 11/9/17.
 */

class DatabaseManager(private val dbHelper: SupportSQLiteOpenHelper) {

    private var database: SupportSQLiteDatabase? = null

    @Synchronized
    fun openDatabase(): SupportSQLiteDatabase {
        if (database == null) {
            // Opening new database
            database = dbHelper.writableDatabase
        }
        return database!!
    }


    fun queryCursor(sql: String, selectionArgs: Array<String>?): Cursor {
        return database!!.query(sql, selectionArgs)
    }

    fun insert(table: String, nullColumnHack: String?, contentValues: ContentValues): Long {
        return database!!.insert(table, SQLiteDatabase.CONFLICT_REPLACE, contentValues)
    }

    fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String>): Int {
        return database!!.update(table, SQLiteDatabase.CONFLICT_REPLACE, values, whereClause, whereArgs)
    }

    fun delete(table: String, whereClause: String, whereArgs: Array<String>): Int {
        return database!!.delete(table, whereClause, whereArgs)
    }


}
