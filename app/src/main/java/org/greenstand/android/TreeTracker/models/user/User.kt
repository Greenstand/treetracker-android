package org.greenstand.android.TreeTracker.models.user

data class User(
    val id: Long,
    val wallet: String,
    val firstName: String,
    val lastName: String?,
    val photoPath: String,
    val isPowerUser: Boolean,
)