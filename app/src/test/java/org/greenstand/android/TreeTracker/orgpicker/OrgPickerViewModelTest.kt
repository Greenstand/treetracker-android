package org.greenstand.android.TreeTracker.orgpicker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class OrgPickerViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private lateinit var orgPickerViewModel: OrgPickerViewModel

    @Before
    fun setup(){
        orgPickerViewModel = OrgPickerViewModel(orgRepo)
    }

    @Test
    @Throws(Exception::class)
    fun `set fake organization, returns success with correct data`()= runBlockingTest {
        val currentOrg = FakeFileGenerator.fakeOrganizationList.first()
        val orgList = FakeFileGenerator.fakeOrganizationList
        //Given
        coEvery { orgRepo.currentOrg() } returns  currentOrg
        coEvery { orgRepo.getOrgs() } returns orgList

        // When
        orgPickerViewModel.setOrg(FakeFileGenerator.fakeOrganizationList.first())

        //Assert LiveData has correct data and verify Org gets the correct set
        val result = orgPickerViewModel.state.getOrAwaitValueTest().currentOrg
        coVerify { orgRepo.getOrgs() }
        Assert.assertEquals(result, FakeFileGenerator.fakeOrganizationList.first())
    }

}