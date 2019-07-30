package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.database.v2.AppDatabaseV2
import org.koin.dsl.module

val roomModule = module {

    single { AppDatabase.getInstance(get()) }

    single { AppDatabaseV2.getInstance(get()) }

    single { AppDatabaseV2.getInstance(get()).treeTrackerDao() }
}