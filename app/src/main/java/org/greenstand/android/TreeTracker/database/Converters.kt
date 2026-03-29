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
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Converters {

    val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun jsonToMap(value: String?): Map<String, String>? {
        if (value == null) return null
        return json.decodeFromString<Map<String, String>>(value)
    }

    @TypeConverter
    fun mapToJson(map: Map<String, String>?): String? {
        if (map == null) return null
        return json.encodeToString(map)
    }

    @TypeConverter
    fun instantToString(instant: Instant?): String? {
        return instant?.let { it.toString() }
    }

    @TypeConverter
    fun stringToInstance(s: String?): Instant? = s?.toInstant()

    @TypeConverter
    fun stringToArray(value: String?): List<String?>? {
        if (value == null) return null
        return json.decodeFromString<List<String?>>(value)
    }

    @TypeConverter
    fun arrayToString(list: List<String?>?): String? {
        if (list == null) return null
        return json.encodeToString(list)
    }
}