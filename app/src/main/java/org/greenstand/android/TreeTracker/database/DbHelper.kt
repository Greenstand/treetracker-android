package org.greenstand.android.TreeTracker.database

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

import org.greenstand.android.TreeTracker.BuildConfig
import timber.log.Timber

import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class DbHelper(private val myContext: Context, name: String, factory: CursorFactory?,
               version: Int) : SQLiteOpenHelper(myContext, name, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // TODO Auto-generated method stub
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // TODO Auto-generated method stub

    }


    /**
     * Creates a empty database on the system and rewrites it with your own database.
     */
    @Throws(IOException::class)
    fun createDataBase() {


        // TODO: Check if the v1 database is in place and copy trees to new database if so
        // just copy the un-synced trees, the photos, and notes
        // copy from DB_NAME_V1 to DB_NAME_V2

        val dbExist = checkDataBase()

        if (dbExist) {
            //do nothing - database already exist
        } else {

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.readableDatabase

            try {

                copyDataBase()

            } catch (e: IOException) {
                Timber.d(e.stackTrace.toString())
                throw Error("Error copying database")

            }

        }

    }


    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private fun checkDataBase(): Boolean {

        var checkDB: SQLiteDatabase? = null

        try {
            val myPath = myContext.getDatabasePath(DB_NAME_V2).path
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)

        } catch (e: SQLiteException) {

            //database does't exist yet.

        }

        if (checkDB != null) {

            checkDB.close()

        }

        return if (checkDB != null) true else false
    }


    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    @Throws(IOException::class)
    private fun copyDataBase() {

        //Open your local db as the input stream
        val myInput = myContext.assets.open(DB_NAME_V2)

        // Path to the just created empty db
        val outFileName = myContext.getDatabasePath(DB_NAME_V2).path

        //Open the empty db as the output stream
        val myOutput = FileOutputStream(outFileName)

        //transfer bytes from the inputfile to the outputfile
        val buffer = ByteArray(1024)
        var length: Int
        length = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }

        //Close the streams
        myOutput.flush()
        myOutput.close()
        myInput.close()

    }

    companion object {

        private val TAG = "SQLiteOpenHelper"
        private val DB_NAME_V1 = "treetracker.db"
        private val DB_NAME_V2 = "treetracker.v2.db"


        fun getDbHelper(context: Context): DbHelper {
            return DbHelper(context, DB_NAME_V2, null, 1)
        }
    }


}
