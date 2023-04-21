/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import java.lang.reflect.Type

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

    @TypeConverter
    fun stringToArray(value: String?): List<String?>? {
        val listType: Type = object : TypeToken<List<String?>?>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun arrayToString(list: List<String?>?): String? {
        return gson.toJson(list)
    }
}