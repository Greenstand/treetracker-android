package org.greenstand.android.TreeTracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat


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

            try {
                copyDataBase()
            } catch (e: IOException) {
                Timber.d(e.stackTrace.toString())
                throw Error("Error copying database")

            }

            val sharedPreferences = myContext.getSharedPreferences(
                    "org.greenstand.android", Context.MODE_PRIVATE)
            val v1DatabaseChecked = sharedPreferences.getBoolean(ValueHelper.V1_DATABASE_CHECKED, false)

            if(checkV1DataBase() && !v1DatabaseChecked) {
                // The V1 database is still here
                val db1Helper = getDbV1Helper(myContext)
                val db2Helper = getDbHelper(myContext)
                val query = "SELECT " +
                        "tree._id as tree_id, " +
                        "tree.time_created as tree_time_created, " +
                        "tree.is_synced, " +
                        "location.lat, " +
                        "location.long, " +
                        "location.accuracy, " +
                        "photo.name, " +
                        "note.content " +
                        "FROM tree " +
                        "LEFT OUTER JOIN location ON location._id = tree.location_id " +
                        "LEFT OUTER JOIN tree_photo ON tree._id = tree_photo.tree_id " +
                        "LEFT OUTER JOIN photo ON photo._id = tree_photo.photo_id " +
                        "LEFT OUTER JOIN tree_note ON tree._id = tree_note.tree_id " +
                        "LEFT OUTER JOIN note ON note._id = tree_note.note_id " +
                        "WHERE " +
                        "is_synced = 'N'"
                val treesCursor = db1Helper.readableDatabase.rawQuery(query, null)
                while (treesCursor.moveToNext()) {


                    val sharedPreferences = myContext.getSharedPreferences(
                            ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
                    val email = sharedPreferences.getString(ValueHelper.USERNAME, "Unknown")


                    val locationContentValues = ContentValues()
                    locationContentValues.put("accuracy",
                            treesCursor.getFloat(treesCursor.getColumnIndex("accuracy")))
                    locationContentValues.put("lat",
                            treesCursor.getDouble(treesCursor.getColumnIndex("lat")))
                    locationContentValues.put("long",
                            treesCursor.getDouble(treesCursor.getColumnIndex("long")))

                    val locationId = db2Helper.writableDatabase.insert("location", null, locationContentValues)

                    Timber.d("locationId " + java.lang.Long.toString(locationId))

                    var photoId: Long = -1

                    // photo
                    val photoContentValues = ContentValues()
                    photoContentValues.put("location_id", locationId)
                    photoContentValues.put("name", treesCursor.getShort(treesCursor.getColumnIndex("name")))

                    photoId = db2Helper.writableDatabase.insert("photo", null, photoContentValues)
                    Timber.d("photoId " + java.lang.Long.toString(photoId))



                    // note
                    val noteContentValues = ContentValues()
                    noteContentValues.put("content", treesCursor.getShort(treesCursor.getColumnIndex("content")))

                    val noteId = db2Helper.writableDatabase.insert("note", null, noteContentValues)
                    Timber.d("noteId " + java.lang.Long.toString(noteId))



                    val treeContentValues = ContentValues()
                    treeContentValues.put("location_id", locationId)
                    treeContentValues.put("planter_identification_id", email)

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                    treeContentValues.put("time_created", treesCursor.getString(treesCursor.getColumnIndex("time_created")))
                    treeContentValues.put("time_updated", treesCursor.getString(treesCursor.getColumnIndex("time_updated")))
                    treeContentValues.put("time_for_update", treesCursor.getString(treesCursor.getColumnIndex("time_for_update")))

                    val treeId = db2Helper.writableDatabase.insert("tree", null, treeContentValues)
                    Timber.d("treeId", java.lang.Long.toString(treeId))

                    // tree_photo
                    val treePhotoContentValues = ContentValues()
                    treePhotoContentValues.put("tree_id", treeId)
                    treePhotoContentValues.put("photo_id", photoId)
                    val treePhotoId = db2Helper.writableDatabase.insert("tree_photo", null, treePhotoContentValues)
                    Timber.d("treePhotoId " + java.lang.Long.toString(treePhotoId))


                    // tree_note
                    val treeNoteContentValues = ContentValues()
                    treeNoteContentValues.put("tree_id", treeId)
                    treeNoteContentValues.put("note_id", noteId)
                    val treeNoteId = db2Helper.writableDatabase.insert("tree_note", null, treeNoteContentValues)
                    Timber.d("treeNoteId " + java.lang.Long.toString(treeNoteId))


                    db2Helper.writableDatabase.insert("tree", null, treeContentValues)
                }

                val editor = sharedPreferences.edit()
                editor.putBoolean(ValueHelper.V1_DATABASE_CHECKED, true)
                editor.commit()
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
            val file = myContext.getDatabasePath(DB_NAME_V2)
            if(file.exists()) {
                val myPath = file.path
                checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: SQLiteException) {

            //database does't exist yet.

        }

        checkDB?.close()

        return checkDB != null
    }


    private fun checkV1DataBase(): Boolean {

        var checkDB: SQLiteDatabase? = null

        try {
            val file = myContext.getDatabasePath(DB_NAME_V1)
            if(file.exists()) {
                checkDB = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: SQLiteException) {

            //database does't exist yet.

        }

        if (checkDB != null) {

            checkDB.close()

        }

        return checkDB != null
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

        private const val DB_NAME_V1 = "treetracker.db"
        private const val DB_NAME_V2 = "treetracker.v2.db"


        fun getDbHelper(context: Context): DbHelper {
            return DbHelper(context, DB_NAME_V2, null, 1)
        }

        fun getDbV1Helper(context: Context): DbHelper {
            return DbHelper(context, DB_NAME_V1, null, 1)
        }
    }


}
