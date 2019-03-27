package org.greenstand.android.TreeTracker.database.dao

class TreeDto {
    var tree_id: Long = 0
    var tree_time_created: String? = null
    var tree_time_updated: String? = null
    var time_for_update: String? = null
    var isTreeSynced: Boolean = false
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var accuracy: Int = 0
    var name: String? = null
    var note: String? = null
    var isOutdated: Boolean? = false
    var planter_identifier: String? = null
    var planter_photo_path: String? = null
    var planter_photo_url: String? = null
    var planter_identifications_id: String? = null

    var height_color: String? = null
    var flavor_id: String? = null
    var app_version: String? = null
    var app_build: String? = null
}

