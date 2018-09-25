package org.greenstand.android.TreeTracker.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * Created by lei on 11/9/17.
 */

class DatabaseManager (private val mDbHelper: DbHelper) {
    private var mDatabase: SQLiteDatabase? = null

    @Synchronized
    fun openDatabase(): SQLiteDatabase {
        if (mDatabase == null) {
            // Opening new database
            mDatabase = mDbHelper.writableDatabase
        }
        return mDatabase!!
    }


    fun queryCursor(sql: String, selectionArgs: Array<String>?): Cursor {
        return mDatabase!!.rawQuery(sql, selectionArgs)
    }

    fun insert(table: String, nullColumnHack: String?, contentValues: ContentValues): Long {
        return mDatabase!!.insert(table, nullColumnHack, contentValues)
    }

    fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String>): Int {
        return mDatabase!!.update(table, values, whereClause, whereArgs)
    }

    fun delete(table: String, whereClause: String, whereArgs: Array<String>): Int {
        return mDatabase!!.delete(table, whereClause, whereArgs)
    }


}
