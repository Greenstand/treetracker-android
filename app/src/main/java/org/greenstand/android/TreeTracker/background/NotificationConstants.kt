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
package org.greenstand.android.TreeTracker.background

object NotificationConstants {
    // SyncNotificationManager
    const val SYNC_NOTIFICATION_ID = 212
    const val SYNC_CHANNEL_ID = "org.greenstand.android.TreeTracker.background.SyncNotificationManager"
    const val SYNC_CHANNEL_NAME = "Sync Channel"
    const val SYNC_CHANNEL_DESCRIPTION = "Tree Syncing"

    // TreeSyncWorker
    const val UNIQUE_WORK_ID = "UNIQUE_WORK_ID_TREE_SYNC_WORKER"
    const val WORKER_NOTIFICATION_ID = 1337
    const val WORKER_CHANNEL_ID = "11"
    const val WORKER_CHANNEL_NAME = "Work Service"
}