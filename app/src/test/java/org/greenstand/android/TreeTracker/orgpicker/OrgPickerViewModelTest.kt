package org.greenstand.android.TreeTracker.orgpicker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class OrgPickerViewModelTest {
    /*
     Here the Rule runs coroutines on the main thread
     */

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val orgRepo = Mockito.mock(OrgRepo::class.java)

    private lateinit var orgPickerViewModel: OrgPickerViewModel

    /*
     Setting the main dispatcher to Dispatcher.Unconfined and
     initializing the treeImageReviewViewModel object.
     */

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        orgPickerViewModel = OrgPickerViewModel(orgRepo)
    }

    /*
     Reset the main dispatcher after each test.
     */

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /*
     Test if setOrg sets to the current org to null.
     */

    @Test
    fun `test for set org`() = runBlockingTest {
        val org = null
        org?.let { orgPickerViewModel.setOrg(it) }
        val actualState = orgPickerViewModel.state.getOrAwaitValueTest()
        assertEquals(org, actualState.currentOrg)
    }
}