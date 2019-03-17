package org.greenstand.android.TreeTracker.database.dao

class TreeDto {
    var tree_id: Long = 0
    lateinit var tree_time_created: String
    var isTreeSynced: Boolean = false
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var accuracy: Int = 0
    var name: String? = null
    var content: String? = null
    var planter_identifier: String? = null
    var planter_photo_path: String? = null
    var planter_photo_url: String? = null
    var planter_identifications_id: String? = null

}

