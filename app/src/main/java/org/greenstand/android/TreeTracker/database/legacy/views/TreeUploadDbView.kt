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
package org.greenstand.android.TreeTracker.database.legacy.views

// @DatabaseView("""
//        SELECT
//        tree_capture.uuid,
//        tree_capture._id as treeCaptureId,
//        tree_capture.photo_url as "treePhotoUrl",
//        tree_capture.created_at,
//        tree_capture.note_content,
//        tree_capture.uploaded,
//        tree_capture.latitude,
//        tree_capture.longitude,
//        tree_capture.accuracy,
//        planter_check_in.planterInfoId as planterInfoId,
//        planter_info._id as planterInfoId,
//        planter_check_in.photo_url as "planterPhotoUrl"
//        from tree_capture
//        inner join planter_check_in on tree_capture.planter_checkin_id = planter_check_in._id
//        inner join planter_info on planter_check_in.planter_info_id = planter_info._id""")
// data class TreeUploadDbView(
//    @ColumnInfo(name = TreeCaptureEntity.UUID)
//    var uuid: String,
//    @ColumnInfo(name = "planterInfoId")
//    var planterInfoId: String,
//    @ColumnInfo(name = "treePhotoUrl")
//    var treePhotoUrl: String,
//    @ColumnInfo(name = "planterInfoId") // TODO CHECK WHAT THIS VALUE SHOULD BE
//    var planterInfoId: String,
//    @ColumnInfo(name = "treeCaptureId")
//    var treeCaptureId: Long,
//    @ColumnInfo(name = "planterPhotoUrl")
//    var planterPhotoUrl: String,
//    @ColumnInfo(name = TreeCaptureEntity.LATITUDE)
//    var latitude: Double,
//    @ColumnInfo(name = TreeCaptureEntity.LONGITUDE)
//    var longitude: Double,
//    @ColumnInfo(name = TreeCaptureEntity.NOTE_CONTENT)
//    var noteContent: String,
//    @ColumnInfo(name = TreeCaptureEntity.ACCURACY)
//    var accuracy: Double,
//    @ColumnInfo(name = TreeCaptureEntity.CREATED_AT)
//    var createAt: Long
// )