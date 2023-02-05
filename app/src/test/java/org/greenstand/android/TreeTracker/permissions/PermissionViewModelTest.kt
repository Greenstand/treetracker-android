package org.greenstand.android.TreeTracker.permissions

import android.location.LocationManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class PermissionViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val locationManager = mockk<LocationManager>(relaxed = true)
    private lateinit var testSubject: PermissionViewModel

    @Before
    fun setup(){
        testSubject = PermissionViewModel(locationManager)
    }
}