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

import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.CreateLegacyTreeUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.usecases.UploadLocationDataUseCase
import org.koin.dsl.module

val useCaseModule =
    module {

        factory { UploadImageUseCase(get()) }

        factory { UploadLocationDataUseCase(get(), get()) }

        factory { CreateTreeUseCase(get(), get(), get()) }

        factory { CreateLegacyTreeUseCase(get(), get()) }

        factory { CreateFakeTreesUseCase(get(), get(), get(), get(), get(), get(), get()) }

        factory { CheckForInternetUseCase() }

        factory { CreateTreeRequestUseCase(get()) }

        factory { SyncDataUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    }