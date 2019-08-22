package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.database.AppDatabase
import org.koin.dsl.module

val roomModule = module {

    single { AppDatabase.getInstance(get()) }

    single { AppDatabase.getInstance(get()).treeTrackerDao() }
}