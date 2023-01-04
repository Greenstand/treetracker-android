package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.utils.*
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
    @Throws(Exception::class)
    fun `insert planterInfo to App Database`() = runBlocking {
        treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        val planterInfo = treeTrackerDAO.getAllPlanterInfo().first().first()
        Assert.assertEquals(fakePlanterInfo, planterInfo)
        Assert.assertNotNull(planterInfo)
    }

    @Test
    @Throws(Exception::class)
    fun `insert userInfo to App Database`() = runBlocking {
        treeTrackerDAO.insertUser(fakeUser)
        val userInfo = treeTrackerDAO.getAllUsers().first().first()
        Assert.assertEquals(fakeUser, userInfo)
        Assert.assertNotNull(userInfo)
    }

    @Test
    fun `insert org to App Database, returns valid org when querying id and name`() = runBlocking {
        treeTrackerDAO.insertOrg(fakeOrg.first())
        val organization = treeTrackerDAO.getOrg("new")
        Assert.assertEquals(organization?.id, fakeOrg.first().id)
        Assert.assertEquals(fakeOrg.first().name, organization?.name)
    }

    @Test
    @Throws(Exception::class)
    fun `saving org to App Database, returns non null data`() = runBlocking {
        treeTrackerDAO.insertOrg(fakeOrg.first())
        val organization = treeTrackerDAO.getOrg("new")
        Assert.assertNotNull(organization)
    }


    @Test
    @Throws(Exception::class)
    fun `insert fake Device Config to App Database, returns non_null and same UUID`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.getLatestDeviceConfig()
        Assert.assertNotNull(deviceConfig)
        Assert.assertEquals(fakeDeviceConfig.uuid, deviceConfig?.uuid)
    }



    @After
    @Throws(IOException::class)
    fun tearDown(){
        database.close()
    }
}