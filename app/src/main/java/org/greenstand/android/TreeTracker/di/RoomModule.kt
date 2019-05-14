package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.koin.dsl.module

val roomModule = module {

    single { AppDatabase.getInstance(get()) }

    single { TreeManager(get()) }

}