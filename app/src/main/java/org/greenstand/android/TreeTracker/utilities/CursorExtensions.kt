package org.greenstand.android.TreeTracker.utilities

import android.database.Cursor

fun Cursor.loadString(id: String): String? = getString(getColumnIndex(id))
fun Cursor.loadFloat(id: String): Float = getFloat(getColumnIndex(id))
fun Cursor.loadLong(id: String): Long = getLong(getColumnIndex(id))
fun Cursor.loadDouble(id: String): Double = getDouble(getColumnIndex(id))