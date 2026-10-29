/*
 * Copyright 2026 Treetracker
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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Before

/**
 * Base class for DAO tests: opens a fresh in-memory [AppDatabase] before each test and closes it after.
 * Subclasses add `@RunWith(RobolectricTestRunner::class)` and `@Config(manifest = Config.NONE)`.
 */
abstract class InMemoryDatabaseTest {
    protected lateinit var database: AppDatabase

    @Before
    fun openDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }
}

/**
 * Collects this flow's first value, runs [change], and returns the first value together with the
 * value emitted after [change]. Used to check that a Room flow re-emits when its tables change.
 */
suspend fun <T> Flow<T>.valuesBeforeAndAfter(change: suspend () -> Unit): Pair<T, T> =
    coroutineScope {
        val firstEmitted = CompletableDeferred<Unit>()
        val emissions =
            async {
                onEach { firstEmitted.complete(Unit) }
                    .take(2)
                    .toList()
            }
        firstEmitted.await()
        change()
        val values = emissions.await()
        values[0] to values[1]
    }