package org.greenstand.android.TreeTracker.sessionnote

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class SessionNoteViewModelTest{
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val captureSetupScopeManager = mockk<CaptureSetupScopeManager>(relaxed = true)

    private lateinit var testSubject: SessionNoteViewModel

    @Before
    fun setup(){
        every { captureSetupScopeManager.getData().user } returns FakeFileGenerator.fakeUsers[1]
        every { captureSetupScopeManager.getData().user?.photoPath } returns FakeFileGenerator.fakeUsers[0].photoPath
        testSubject = SessionNoteViewModel()
    }

    @Test
    fun `Random test block`()= runTest {
        testSubject.updateNote("testing note")
        testSubject.state.test {
            assertEquals(awaitItem().note, "test note")
        }
    }
}