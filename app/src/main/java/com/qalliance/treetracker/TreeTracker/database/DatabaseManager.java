package com.qalliance.treetracker.TreeTracker.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by lei on 11/9/17.
 */

public class DatabaseManager {

    private static DatabaseManager sInstance;
    private DbHelper mDbHelper;
    private SQLiteDatabase mDatabase;
    private int mOpenCounter;

    public static synchronized DatabaseManager getInstance(DbHelper dbHelper) {
        if (sInstance == null) {
            sInstance = new DatabaseManager(dbHelper);
        }

        return sInstance;
    }

    private DatabaseManager(DbHelper dbHelper) {
        mDbHelper = dbHelper;
    }

    public synchronized SQLiteDatabase openDatabase() {
        mOpenCounter++;
        if(mOpenCounter == 1) {
            // Opening new database
            mDatabase = mDbHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        mOpenCounter--;
        if(mOpenCounter == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    public Cursor queryCursor(String sql, String[] selectionArgs) {
        return mDatabase.rawQuery(sql, selectionArgs);
    }

    public long insert(String table, String nullColumnHack, ContentValues contentValues) {
        return mDatabase.insert(table, nullColumnHack, contentValues);
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return mDatabase.update(table, values, whereClause, whereArgs);
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        return mDatabase.delete(table, whereClause, whereArgs);
    }
}
