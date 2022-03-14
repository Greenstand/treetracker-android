package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.models.messages.database.MessageDatabase
import org.koin.dsl.module

val roomModule = module {

    single { AppDatabase.getInstance(get()) }

    single { AppDatabase.getInstance(get()).treeTrackerDao() }

    single { MessageDatabase.getInstance(get()) }

    single { MessageDatabase.getInstance(get()).messagesDao() }
}