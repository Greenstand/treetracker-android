package org.greenstand.android.TreeTracker.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant

object Converters {

    val gson = Gson()

    @TypeConverter
    fun jsonToMap(value: String?): Map<String, String>? {
        return gson.fromJson(value, Map::class.java) as? Map<String, String>
    }

    @TypeConverter
    fun mapToJson(map: Map<String, String>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun instantToString(instant: Instant?): String? {
        return instant?.let { it.toString() }
    }

    @TypeConverter
    fun stringToInstance(s: String?): Instant? = s?.toInstant()
}