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
package org.greenstand.android.TreeTracker.di

import androidx.room.Room
import net.sqlcipher.database.SupportFactory
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.database.app.AppDatabase
import org.greenstand.android.TreeTracker.database.app.MIGRATION_3_4
import org.greenstand.android.TreeTracker.database.app.MIGRATION_4_5
import org.greenstand.android.TreeTracker.database.app.MIGRATION_5_6
import org.greenstand.android.TreeTracker.database.app.MIGRATION_6_7
import org.greenstand.android.TreeTracker.database.common.Encrypt.encrypt
import org.greenstand.android.TreeTracker.database.messages.MessageDatabase
import org.koin.dsl.module

val roomModule = module {

    single {
        encrypt(
            context = get(),
            oldName = AppDatabase.DB_NAME,
            newName = AppDatabase.DB_NAME_ENCRYPT
        )
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            AppDatabase.DB_NAME_ENCRYPT
        ).addMigrations(
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        ).openHelperFactory(
            if (BuildConfig.DEBUG) null else SupportFactory(BuildConfig.CRYPTO_KEY.toByteArray())
        ).build()
    }

    single { get<AppDatabase>().treeTrackerDao() }

    single {
        encrypt(
            context = get(),
            oldName = MessageDatabase.DB_NAME,
            newName = MessageDatabase.DB_NAME_ENCRYPT
        )
        Room.databaseBuilder(
            get(),
            MessageDatabase::class.java,
            MessageDatabase.DB_NAME_ENCRYPT
        ).openHelperFactory(
            if (BuildConfig.DEBUG) null else SupportFactory(BuildConfig.CRYPTO_KEY.toByteArray())
        ).build()
    }

    single { get<MessageDatabase>().messagesDao() }
}