package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.managers.PlanterManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.koin.dsl.module

val userModule = module {

    single { UserManager() }

    single { PlanterManager(get()) }

}