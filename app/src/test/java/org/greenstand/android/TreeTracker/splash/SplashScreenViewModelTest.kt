package org.greenstand.android.TreeTracker.splash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SplashScreenViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private var orgJsonString : String? = null

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
    private lateinit var exceptionDataCollector: ExceptionDataCollector

    private lateinit var splashScreenViewModel: SplashScreenViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        splashScreenViewModel = SplashScreenViewModel(
            orgJsonString = orgJsonString,
            userRepo = userRepo,
            treesToSyncHelper = treesToSyncHelper,
            sessionTracker = sessionTracker,
            deviceConfigUpdater = deviceConfigUpdater,
            locationDataCapturer = locationDataCapturer,
            messagesRepo = messagesRepo,
            checkForInternetUseCase = checkForInternetUseCase,
            orgRepo = orgRepo,
            exceptionDataCollector = exceptionDataCollector
        )
    }

    @Test
    fun `WHEN every condition in a function is true THEN entire body of the function is executed`() = runBlocking {
        val user = FakeFileGenerator.emptyUser
        orgJsonString = "json string"
        coEvery { checkForInternetUseCase.execute(Unit) } returns true
        coEvery { userRepo.getPowerUser() } returns user
        every { sessionTracker.wasSessionInterrupted() } returns true
        every { treesToSyncHelper.getTreeCountToSync() } returns -1

        splashScreenViewModel.bootstrap()

        coVerify(exactly = 1) { deviceConfigUpdater.saveLatestConfig() }
        coVerify(exactly = 1) { orgRepo.init() }
        coVerify(exactly = 0) { orgRepo.addOrgFromJsonString(orgJsonString ?: "some string") }
        coVerify(exactly = 1) { messagesRepo.syncMessages() }
        coVerify(exactly = 1) { exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET,user.wallet) }
        coVerify(exactly = 1) { treesToSyncHelper.refreshTreeCountToSync() }
    }

    @Test
    fun `functions are not executed where condition is false`() = runBlocking {
        val user = FakeFileGenerator.emptyUser
        orgJsonString = null
        coEvery { checkForInternetUseCase.execute(Unit) } returns false
        coEvery { userRepo.getPowerUser() } returns null
        every { sessionTracker.wasSessionInterrupted() } returns false
        every { treesToSyncHelper.getTreeCountToSync() } returns 1

        splashScreenViewModel.bootstrap()

        coVerify(exactly = 1) { deviceConfigUpdater.saveLatestConfig() }
        coVerify(exactly = 1) { orgRepo.init() }
        coVerify(exactly = 0) { orgRepo.addOrgFromJsonString(orgJsonString ?: "some stirng") }
        coVerify(exactly = 0) { messagesRepo.syncMessages() }
        coVerify(exactly = 0) { exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET,user.wallet) }
        coVerify(exactly = 0) { treesToSyncHelper.refreshTreeCountToSync() }
    }

    @Test
    fun `WHEN User object is null THEN isInitialSetupRequired is true`() = runBlocking{
        coEvery { userRepo.getPowerUser() } returns null

        val result =  splashScreenViewModel.isInitialSetupRequired()

        assertTrue(result)
    }

    @Test
    fun `WHEN User object is not  null THEN isInitialSetupRequired is false`() = runBlocking{
        val user = FakeFileGenerator.emptyUser
        coEvery { userRepo.getPowerUser() } returns user

        val result =  splashScreenViewModel.isInitialSetupRequired()

        assertFalse(result)
    }

    @Test
    fun `WHEN function startGPSUpdatesForSignup() is called THEN function startGpsUpdates() is executed 1 time`() = runBlocking {
        splashScreenViewModel.startGPSUpdatesForSignup()
        verify(exactly = 1) { locationDataCapturer.startGpsUpdates() }
    }

}