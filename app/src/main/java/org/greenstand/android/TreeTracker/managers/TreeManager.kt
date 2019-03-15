package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.application.TreeTrackerApplication

/**
 * This is a Singleton class
 *
 * All interaction with the tree database should be done in this class moving forward
 *
 * If a method to access some tree data is not in this class, please make it
 */
object TreeManager {

    private val dbManager by lazy { TreeTrackerApplication.getDatabaseManager() }

}