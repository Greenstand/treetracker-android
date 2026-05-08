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

import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.messages.MessageUploader
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.FeatureResolver
import org.greenstand.android.TreeTracker.models.organization.OrgConfigProvider
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.koin.dsl.module

val repositoryModule =
    module {

        single { UserRepo(get(), get(), get(), get(), get(), get()) }

        single { OrgRepo(get(), get(), get()) }

        single { FeatureResolver(get()) }

        single { MessagesRepo(get(), get(), get(), get(), get()) }

        factory { MessageUploader(get(), get(), get()) }

        single { LocationUpdateManager(get(), get(), get()) }

        single { ObjectStorageClient.instance() }

        single {
            LocationDataCapturer(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }

        factory { PlanterUploader(get(), get(), get(), get()) }

        factory { SessionUploader(get(), get(), get()) }

        factory { DeviceConfigUploader(get(), get(), get()) }

        factory { TreeUploader(get(), get(), get(), get(), get()) }

        single { OrgConfigProvider(get()) }
    }
