package org.greenstand.android.TreeTracker.languagepicker

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.junit.Before
import org.junit.Rule

@ExperimentalCoroutinesApi
class LanguagePickerViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val languageSwitcher = mockk<LanguageSwitcher>(relaxed = true)
    private val resources = mockk<Resources>()

    private lateinit var testSubject: LanguagePickerViewModel

    @Before
    fun setup(){
        testSubject = LanguagePickerViewModel(languageSwitcher, resources)
    }
}