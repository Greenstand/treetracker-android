package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.utils.emptyPlanterInfo
import org.greenstand.android.TreeTracker.utils.fakePlanterInfo
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.IOException


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreeTrackerDaoTest {

    private lateinit var treeTrackerDAO: TreeTrackerDAO
    private lateinit var database: AppDatabase

    @Before
    fun createDb(){
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        treeTrackerDAO = database.treeTrackerDao()
    }

    @Test
    fun `insert planterInfo to App Database`() = runBlocking {
        treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        val planterInfo = treeTrackerDAO.getAllPlanterInfo().first().first()
        Assert.assertEquals(planterInfo, fakePlanterInfo)
    }

    @Test
    fun `delete planterInfo in App Database`() = runBlocking {
        treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        treeTrackerDAO.deletePlanterInfo(fakePlanterInfo)
        val planterInfo = treeTrackerDAO.getAllPlanterInfo()
        Assert.assertNull(planterInfo)
    }

    @After
    @Throws(IOException::class)
    fun tearDown(){
        database.close()
    }
}