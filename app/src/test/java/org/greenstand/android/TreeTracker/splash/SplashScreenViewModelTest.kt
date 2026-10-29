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
package org.greenstand.android.TreeTracker.splash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.InstallReferrerOrgProvider
import org.greenstand.android.TreeTracker.models.organization.OrgConfigProvider
import org.greenstand.android.TreeTracker.models.organization.OrgLink
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SplashScreenViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private var orgId: String? = null
    private var orgName: String? = null

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    @MockK(relaxed = true)
    private lateinit var deviceConfigUpdater: DeviceConfigUpdater

    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var messagesRepo: MessagesRepo

    @MockK(relaxed = true)
    private lateinit var checkForInternetUseCase: CheckForInternetUseCase

    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo

    @MockK(relaxed = true)
    private lateinit var orgConfigProvider: OrgConfigProvider

    @MockK(relaxed = true)
    private lateinit var exceptionDataCollector: ExceptionDataCollector

    @MockK(relaxed = true)
    private lateinit var installReferrerOrgProvider: InstallReferrerOrgProvider

    private lateinit var splashScreenViewModel: SplashScreenViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { installReferrerOrgProvider.consume() } returns null
        splashScreenViewModel =
            SplashScreenViewModel(
                orgId = orgId,
                orgName = orgName,
                userRepo = userRepo,
                treesToSyncHelper = treesToSyncHelper,
                sessionTracker = sessionTracker,
                deviceConfigUpdater = deviceConfigUpdater,
                locationDataCapturer = locationDataCapturer,
                messagesRepo = messagesRepo,
                checkForInternetUseCase = checkForInternetUseCase,
                orgRepo = orgRepo,
                orgConfigProvider = orgConfigProvider,
                installReferrerOrgProvider = installReferrerOrgProvider,
                exceptionDataCollector = exceptionDataCollector,
            )
    }

    @Test
    fun `WHEN every condition in a function is true THEN entire body of the function is executed`() =
        runTest {
            val user = FakeFileGenerator.emptyUser
            coEvery { checkForInternetUseCase.execute(Unit) } returns true
            coEvery { userRepo.getPowerUser() } returns user
            every { sessionTracker.wasSessionInterrupted() } returns true
            every { treesToSyncHelper.getTreeCountToSync() } returns -1

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 1) { deviceConfigUpdater.saveLatestConfig() }
            coVerify(exactly = 1) { orgRepo.ensureInitialized() }
            coVerify(exactly = 1) { messagesRepo.syncMessages() }
            coVerify(exactly = 1) { exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, user.wallet) }
            coVerify(exactly = 1) { treesToSyncHelper.refreshTreeCountToSync() }
        }

    @Test
    fun `functions are not executed where condition is false`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns false
            coEvery { userRepo.getPowerUser() } returns null
            every { sessionTracker.wasSessionInterrupted() } returns false
            every { treesToSyncHelper.getTreeCountToSync() } returns 1

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 1) { deviceConfigUpdater.saveLatestConfig() }
            coVerify(exactly = 1) { orgRepo.ensureInitialized() }
            coVerify(exactly = 0) { orgRepo.addOrgFromRemoteConfig(any(), any(), any()) }
            coVerify(exactly = 0) { orgRepo.addMinimalOrg(any(), any()) }
            coVerify(exactly = 0) { messagesRepo.syncMessages() }
            coVerify(exactly = 0) { treesToSyncHelper.refreshTreeCountToSync() }
        }

    @Test
    fun `WHEN User object is null THEN isInitialSetupRequired is true`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null

            val result = splashScreenViewModel.isInitialSetupRequired()

            assertTrue(result)
        }

    @Test
    fun `WHEN User object is not  null THEN isInitialSetupRequired is false`() =
        runTest {
            val user = FakeFileGenerator.emptyUser
            coEvery { userRepo.getPowerUser() } returns user

            val result = splashScreenViewModel.isInitialSetupRequired()

            assertFalse(result)
        }

    @Test
    fun `WHEN function startGPSUpdatesForSignup() is called THEN function startGpsUpdates() is executed 1 time`() =
        runTest {
            splashScreenViewModel.handleAction(SplashAction.StartGPSUpdatesForSignup)
            verify(exactly = 1) { locationDataCapturer.startGpsUpdates() }
        }

    // --- Deeplink org config path tests ---

    private fun createViewModelWithOrg(
        id: String?,
        name: String? = null,
    ) = SplashScreenViewModel(
        orgId = id,
        orgName = name,
        userRepo = userRepo,
        treesToSyncHelper = treesToSyncHelper,
        sessionTracker = sessionTracker,
        deviceConfigUpdater = deviceConfigUpdater,
        locationDataCapturer = locationDataCapturer,
        messagesRepo = messagesRepo,
        checkForInternetUseCase = checkForInternetUseCase,
        orgRepo = orgRepo,
        orgConfigProvider = orgConfigProvider,
        installReferrerOrgProvider = installReferrerOrgProvider,
        exceptionDataCollector = exceptionDataCollector,
    )

    @Test
    fun `WHEN deeplink has orgId and Remote Config returns config THEN calls addOrgFromRemoteConfig`() =
        runTest {
            val vm = createViewModelWithOrg(id = "123", name = "TestOrg")
            val configJson = """{"version":"1"}"""
            coEvery { orgConfigProvider.fetchOrgConfig("123") } returns configJson
            coEvery { orgRepo.addOrgFromRemoteConfig("123", "TestOrg", configJson) } returns true

            vm.bootstrap()

            coVerify(exactly = 1) { orgConfigProvider.fetchOrgConfig("123") }
            coVerify(exactly = 1) { orgRepo.addOrgFromRemoteConfig("123", "TestOrg", configJson) }
            coVerify(exactly = 0) { orgRepo.addMinimalOrg(any(), any()) }
        }

    @Test
    fun `WHEN deeplink has orgId but Remote Config returns null THEN calls addMinimalOrg`() =
        runTest {
            val vm = createViewModelWithOrg(id = "456", name = "FallbackOrg")
            coEvery { orgConfigProvider.fetchOrgConfig("456") } returns null
            coEvery { orgRepo.addMinimalOrg("456", "FallbackOrg") } returns true

            vm.bootstrap()

            coVerify(exactly = 1) { orgConfigProvider.fetchOrgConfig("456") }
            coVerify(exactly = 0) { orgRepo.addOrgFromRemoteConfig(any(), any(), any()) }
            coVerify(exactly = 1) { orgRepo.addMinimalOrg("456", "FallbackOrg") }
        }

    @Test
    fun `WHEN deeplink has orgId but null orgName THEN passes empty string as name`() =
        runTest {
            val vm = createViewModelWithOrg(id = "789", name = null)
            coEvery { orgConfigProvider.fetchOrgConfig("789") } returns null

            vm.bootstrap()

            coVerify(exactly = 1) { orgRepo.addMinimalOrg("789", "") }
        }

    @Test
    fun `WHEN deeplink has blank orgId THEN skips org config entirely`() =
        runTest {
            val vm = createViewModelWithOrg(id = "  ", name = "Blank")

            vm.bootstrap()

            coVerify(exactly = 0) { orgConfigProvider.fetchOrgConfig(any()) }
            coVerify(exactly = 0) { orgRepo.addOrgFromRemoteConfig(any(), any(), any()) }
            coVerify(exactly = 0) { orgRepo.addMinimalOrg(any(), any()) }
        }

    @Test
    fun `WHEN no deeplink THEN skips org config entirely`() =
        runTest {
            val vm = createViewModelWithOrg(id = null, name = null)

            vm.bootstrap()

            coVerify(exactly = 0) { orgConfigProvider.fetchOrgConfig(any()) }
            coVerify(exactly = 0) { orgRepo.addOrgFromRemoteConfig(any(), any(), any()) }
            coVerify(exactly = 0) { orgRepo.addMinimalOrg(any(), any()) }
        }

    @Test
    fun `WHEN no deeplink but install referrer has an org THEN binds that org`() =
        runTest {
            val vm = createViewModelWithOrg(id = null, name = null)
            coEvery { installReferrerOrgProvider.consume() } returns OrgLink("42", "Acme")
            coEvery { orgConfigProvider.fetchOrgConfig("42") } returns null

            vm.bootstrap()

            coVerify(exactly = 1) { orgRepo.addMinimalOrg("42", "Acme") }
        }

    @Test
    fun `WHEN the deeplink carries an org THEN the install referrer is not read`() =
        runTest {
            val vm = createViewModelWithOrg(id = "123", name = "TestOrg")
            coEvery { orgConfigProvider.fetchOrgConfig("123") } returns null

            vm.bootstrap()

            coVerify(exactly = 0) { installReferrerOrgProvider.consume() }
        }

    // --- Initial state and actions ---

    @Test
    fun `WHEN created THEN state is bootstrapping and no events are emitted`() =
        runTest {
            assertEquals(SplashState(isBootstrapping = true), splashScreenViewModel.state.value)
            assertTrue(splashScreenViewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN created THEN no bootstrap work starts until bootstrap is called`() =
        runTest {
            coVerify(exactly = 0) { deviceConfigUpdater.saveLatestConfig() }
            coVerify(exactly = 0) { orgRepo.ensureInitialized() }
            verify(exactly = 0) { locationDataCapturer.startGpsUpdates() }
        }

    @Test
    fun `WHEN GPS updates are started for signup THEN no bootstrap work runs and state is unchanged`() =
        runTest {
            splashScreenViewModel.handleAction(SplashAction.StartGPSUpdatesForSignup)

            coVerify(exactly = 0) { deviceConfigUpdater.saveLatestConfig() }
            assertEquals(SplashState(), splashScreenViewModel.state.value)
            assertTrue(splashScreenViewModel.events.replayCache.isEmpty())
        }

    // --- Bootstrap ordering ---

    @Test
    fun `WHEN bootstrapping with a deeplink THEN steps run in order`() =
        runTest {
            val vm = createViewModelWithOrg(id = "123", name = "TestOrg")
            coEvery { orgConfigProvider.fetchOrgConfig("123") } returns null
            coEvery { checkForInternetUseCase.execute(Unit) } returns true

            vm.bootstrap()

            coVerifyOrder {
                deviceConfigUpdater.saveLatestConfig()
                orgRepo.ensureInitialized()
                orgConfigProvider.fetchOrgConfig("123")
                orgRepo.addMinimalOrg("123", "TestOrg")
                checkForInternetUseCase.execute(Unit)
                messagesRepo.syncMessages()
                userRepo.getPowerUser()
            }
        }

    // --- Messages and power user ---

    @Test
    fun `GIVEN internet but no power user WHEN bootstrapping THEN syncs messages without tagging a power user`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns true
            coEvery { userRepo.getPowerUser() } returns null

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 1) { messagesRepo.syncMessages() }
            verify(exactly = 0) { exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, any<String>()) }
        }

    @Test
    fun `GIVEN no internet but a power user WHEN bootstrapping THEN tags the power user without syncing messages`() =
        runTest {
            val user = FakeFileGenerator.fakeUsers.first().copy(wallet = "power-wallet")
            coEvery { checkForInternetUseCase.execute(Unit) } returns false
            coEvery { userRepo.getPowerUser() } returns user

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 0) { messagesRepo.syncMessages() }
            verify(exactly = 1) { exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, "power-wallet") }
        }

    // --- Trees-to-sync refresh ---

    @Test
    fun `GIVEN an interrupted session and a known tree count WHEN bootstrapping THEN refreshes the tree count without reading it`() =
        runTest {
            every { sessionTracker.wasSessionInterrupted() } returns true
            every { treesToSyncHelper.getTreeCountToSync() } returns 5

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 1) { treesToSyncHelper.refreshTreeCountToSync() }
            verify(exactly = 0) { treesToSyncHelper.getTreeCountToSync() }
        }

    @Test
    fun `GIVEN no interrupted session and an unknown tree count WHEN bootstrapping THEN refreshes the tree count`() =
        runTest {
            every { sessionTracker.wasSessionInterrupted() } returns false
            every { treesToSyncHelper.getTreeCountToSync() } returns -1

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 1) { treesToSyncHelper.refreshTreeCountToSync() }
        }

    @Test
    fun `GIVEN no interrupted session and zero trees to sync WHEN bootstrapping THEN does not refresh the tree count`() =
        runTest {
            every { sessionTracker.wasSessionInterrupted() } returns false
            every { treesToSyncHelper.getTreeCountToSync() } returns 0

            splashScreenViewModel.bootstrap()

            coVerify(exactly = 0) { treesToSyncHelper.refreshTreeCountToSync() }
        }

    // --- Org link sources ---

    @Test
    fun `WHEN install referrer org has a Remote Config THEN applies the full config`() =
        runTest {
            val vm = createViewModelWithOrg(id = null, name = null)
            val configJson = """{"version":"2"}"""
            coEvery { installReferrerOrgProvider.consume() } returns OrgLink("42", "Acme")
            coEvery { orgConfigProvider.fetchOrgConfig("42") } returns configJson

            vm.bootstrap()

            coVerify(exactly = 1) { orgRepo.addOrgFromRemoteConfig("42", "Acme", configJson) }
            coVerify(exactly = 0) { orgRepo.addMinimalOrg(any(), any()) }
        }

    @Test
    fun `WHEN deeplink orgId is blank THEN falls back to the install referrer org`() =
        runTest {
            val vm = createViewModelWithOrg(id = "  ", name = "Blank")
            coEvery { installReferrerOrgProvider.consume() } returns OrgLink("42", "Acme")
            coEvery { orgConfigProvider.fetchOrgConfig("42") } returns null

            vm.bootstrap()

            coVerify(exactly = 1) { installReferrerOrgProvider.consume() }
            coVerify(exactly = 1) { orgRepo.addMinimalOrg("42", "Acme") }
            coVerify(exactly = 0) { orgRepo.addMinimalOrg(any(), "Blank") }
        }

    @Test
    fun `WHEN deeplink orgId is empty THEN falls back to the install referrer`() =
        runTest {
            val vm = createViewModelWithOrg(id = "", name = "Empty")

            vm.bootstrap()

            coVerify(exactly = 1) { installReferrerOrgProvider.consume() }
            coVerify(exactly = 0) { orgConfigProvider.fetchOrgConfig(any()) }
        }

    @Test
    fun `WHEN deeplink has orgId but no name THEN Remote Config org is added with an empty name`() =
        runTest {
            val vm = createViewModelWithOrg(id = "321", name = null)
            val configJson = """{"version":"1"}"""
            coEvery { orgConfigProvider.fetchOrgConfig("321") } returns configJson

            vm.bootstrap()

            coVerify(exactly = 1) { orgRepo.addOrgFromRemoteConfig("321", "", configJson) }
        }

    @Test
    fun `WHEN bootstrap completes THEN no navigation or other events are emitted`() =
        runTest {
            val vm = createViewModelWithOrg(id = "123", name = "TestOrg")
            coEvery { orgConfigProvider.fetchOrgConfig("123") } returns null

            vm.bootstrap()

            assertTrue(vm.events.replayCache.isEmpty())
            assertEquals(SplashState(), vm.state.value)
        }
}