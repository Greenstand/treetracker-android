package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.utils.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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
    fun `insert planterInfo to App Database, assert non null and returns valid planter Info`() = runBlocking {
        treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        val planterInfo = treeTrackerDAO.getAllPlanterInfo().first().first()
        assertEquals(fakePlanterInfo, planterInfo)
        assertNotNull(planterInfo)
    }

    @Test
    @Throws(Exception::class)
    fun `insert userInfo to App Database, assert non null and returns valid User`() = runBlocking {
        treeTrackerDAO.insertUser(fakeUser)
        val userInfo = treeTrackerDAO.getAllUsers().first().first()
        assertEquals(fakeUser, userInfo)
        assertNotNull(userInfo)
    }

    @Test
    @Throws(Exception::class)
    fun `update user UUID, assert fake UUID not equal to updated user,`() = runBlocking {
        treeTrackerDAO.insertUser(fakeUser)
        val userUpdate = treeTrackerDAO.updateUser(fakeUser.copy(uuid = "newStringUpdate"))
        assertNotEquals(fakeUser.uuid, userUpdate)
    }

    @Test
    @Throws(Exception::class)
    fun `update user Bundle Id, assert fake Bundle, different from Updated,`() = runBlocking {
        treeTrackerDAO.insertUser(fakeUser)
        val userUpdate = treeTrackerDAO.updateUserBundleIds(listOf(12, 344), bundleId = "newString")
        assertNotEquals(fakeUser.bundleId, userUpdate)
    }

    @Test
    fun `insert org to App Database, returns valid org when querying id and name`() = runBlocking {
        treeTrackerDAO.insertOrg(fakeOrg.first())
        val organization = treeTrackerDAO.getOrg("new")
        assertEquals(organization?.id, fakeOrg.first().id)
        assertEquals(fakeOrg.first().name, organization?.name)
    }

    @Test
    @Throws(Exception::class)
    fun `saving org to App Database, returns non null data`() = runBlocking {
        treeTrackerDAO.insertOrg(fakeOrg.first())
        val organization = treeTrackerDAO.getOrg("new")
        assertNotNull(organization)
    }

    @Test
    @Throws(Exception::class)
    fun `insert fake Device Config to App Database, returns non_null and returns valid, querying uuid`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.getLatestDeviceConfig()
        assertNotNull(deviceConfig)
        assertEquals(fakeDeviceConfig.uuid, deviceConfig?.uuid)
    }

    @Test
    @Throws(Exception::class)
    fun `updated device config upload status, assert fake upload status not same as updated status`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.updateDeviceConfigUploadStatus(ids = listOf(12, 12), isUploaded = true)
        assertNotEquals(fakeDeviceConfig.isUploaded, deviceConfig)
    }

    @Test
    @Throws(Exception::class)
    fun `updated device config bundle ID, assert fake bundleId not same as updated bundleID,`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.updateDeviceConfigBundleIds(ids = listOf(15, 16),
            bundleId = "newRandomString")
        assertNotEquals(fakeDeviceConfig.bundleId, deviceConfig)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Session, returns valid with non null data `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        fakeDeviceConfig.id = deviceConfigId
        assertEquals(deviceConfigId, fakeDeviceConfig.id)
        val newSession = fakeSession.copy(deviceConfigId = deviceConfigId)

        val checkIfInserted = treeTrackerDAO.insertSession(newSession)
        assertNotNull(checkIfInserted)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Entity, returns valid with non null data `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        fakeDeviceConfig.id = deviceConfigId

        val newSession = fakeSession.copy(deviceConfigId = deviceConfigId)
        val newSessionId = treeTrackerDAO.insertSession(newSession)
        newSession.id = newSessionId

        assertEquals(newSessionId, newSession.id)
        val newTree = fakeTree.first().copy(sessionId = newSessionId)
        val checkIfInserted = treeTrackerDAO.insertTree(newTree)
        assertNotNull(checkIfInserted)
    }

    @Test
    @Throws(Exception::class)
    fun `update Tree Entity, assert fake tree not same as updated `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        fakeDeviceConfig.id = deviceConfigId

        val newSession = fakeSession.copy(deviceConfigId = deviceConfigId)
        val newSessionId = treeTrackerDAO.insertSession(newSession)
        newSession.id = newSessionId

        assertEquals(newSessionId, newSession.id)
        val newTree = fakeTree.first().copy(sessionId = newSessionId)
        val fakeTree = treeTrackerDAO.insertTree(newTree)
        val updated = newTree.copy(uuid = "testing")
        assertNotEquals(fakeTree, updated)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Planter CheckIn Entity, returns valid with non null data `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val checkIfInserted = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        assertNotNull(checkIfInserted)
    }

    @Test
    @Throws(Exception::class)
    fun `update Planter CheckIn Entity, assert fake planter checkIn not same as updated`() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val fakePlanter = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        val updated = newPlanterCheckIn.copy(latitude = 9888.11)
        assertNotEquals(fakePlanter, updated)
    }

    @Test
    @Throws(Exception::class)
    fun `delete Planter CheckIn Entity, assert null`() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        treeTrackerDAO.deletePlanterCheckIn(newPlanterCheckIn)
        val planter = treeTrackerDAO.getPlanterCheckInById(newPlanterCheckIn.id)
        assertNull(planter)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Location Entity, returns valid with non null data `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(fakeDeviceConfig)
        fakeDeviceConfig.id = deviceConfigId

        val newSession = fakeSession.copy(deviceConfigId = deviceConfigId)
        val newSessionId = treeTrackerDAO.insertSession(newSession)
        newSession.id = newSessionId

        assertEquals(newSessionId, newSession.id)
        val newLocation = fakeLocation.copy(sessionId = newSessionId)
        val checkIfInserted = treeTrackerDAO.insertLocationData(newLocation)
        assertNotNull(checkIfInserted)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Capture Entity, returns valid with non null data `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val planterCheckId = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        newPlanterCheckIn.id = planterCheckId
        val newTreeCapture = fakeTreeCapture.copy(planterCheckInId = planterCheckId)
        val checkIfInserted = treeTrackerDAO.insertTreeCapture(newTreeCapture)
        assertNotNull(checkIfInserted)
    }

    @Test
    @Throws(Exception::class)
    fun `update Tree Capture Entity, assert fake tree capture not same as updated `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val planterCheckId = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        newPlanterCheckIn.id = planterCheckId
        val newTreeCapture = fakeTreeCapture.copy(planterCheckInId = planterCheckId)
        val fakeCapture = treeTrackerDAO.insertTreeCapture(newTreeCapture)
        val updated = newTreeCapture.copy(uuid = "testing")
        assertNotEquals(fakeCapture, updated)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Attribute Entity, returns valid with non null data `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(fakePlanterInfo)
        fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val planterCheckId = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        newPlanterCheckIn.id = planterCheckId
        val newTreeCapture = fakeTreeCapture.copy(planterCheckInId = planterCheckId)
        val newTreeCaptureId = treeTrackerDAO.insertTreeCapture(newTreeCapture)
        newTreeCapture.id = newTreeCaptureId
        val newTreeAttribute = fakeTreeAttribute.copy(treeCaptureId = newTreeCaptureId)
        val checkIfInserted = treeTrackerDAO.insertTreeAttribute(newTreeAttribute)
        assertNotNull(checkIfInserted)
    }
    @After
        @Throws(IOException::class)
        fun tearDown(){
            database.close()
        }
    }
