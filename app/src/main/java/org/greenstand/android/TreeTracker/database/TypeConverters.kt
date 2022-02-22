package org.greenstand.android.TreeTracker.database

import androidx.room.TypeConverter
import com.google.gson.Gson

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
}