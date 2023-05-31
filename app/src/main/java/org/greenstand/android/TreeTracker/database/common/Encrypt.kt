package org.greenstand.android.TreeTracker.database.common

import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import org.greenstand.android.TreeTracker.BuildConfig

object Encrypt {

    fun encrypt(context: Context, oldName: String, newName: String) {
        SQLiteDatabase.loadLibs(context)
        val dbFile = context.getDatabasePath(newName)
        val legacyFile = context.getDatabasePath(oldName)
        if (!dbFile.exists() && legacyFile.exists()) {
            var db = SQLiteDatabase.openOrCreateDatabase(legacyFile, "", null)
            db.rawExecSQL(
                String.format(
                    "ATTACH DATABASE '%s' AS encrypted KEY '%s';",
                    dbFile.absolutePath,
                    if (BuildConfig.DEBUG) "" else BuildConfig.CRYPTO_KEY
                )
            )
            db.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            db.rawExecSQL("DETACH DATABASE encrypted;")
            val version = db.version
            db.close()
            db = SQLiteDatabase.openOrCreateDatabase(
                dbFile,
                if (BuildConfig.DEBUG) "" else BuildConfig.CRYPTO_KEY,
                null
            )
            db.version = version
            db.close()
            legacyFile.delete()
        }
    }

}