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
package org.greenstand.android.TreeTracker.utilities

import android.provider.Settings
import java.util.*
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication

object DeviceUtils {

    val deviceId: String
        get() = Settings.Secure.getString(TreeTrackerApplication.appContext.contentResolver, Settings.Secure.ANDROID_ID)

    val language: String
        get() = Locale.getDefault().displayLanguage
}
