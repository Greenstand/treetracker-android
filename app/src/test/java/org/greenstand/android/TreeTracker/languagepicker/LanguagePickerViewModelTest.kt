package org.greenstand.android.TreeTracker.languagepicker

import android.app.Activity
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LanguagePickerViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val languageSwitcher = mockk<LanguageSwitcher>(relaxed = true)
    private val resources = mockk<Resources>(relaxUnitFun = true)

    private lateinit var testSubject: LanguagePickerViewModel

    @Before
    fun setup(){
        coEvery { languageSwitcher.currentLanguage() } returns Language.ENGLISH
        testSubject = LanguagePickerViewModel(languageSwitcher, resources)
    }

    @Test
    fun `Current language returns current language from the language switcher`()= runBlocking {
        val result = testSubject.currentLanguage.getOrAwaitValueTest()
        Assert.assertEquals(result, Language.ENGLISH)
    }

    @Test
    fun `Verify set language calls the set language from the Language Switcher `()= runBlocking{
        val language = Language.ENGLISH
        testSubject.setLanguage(language)
        coVerify { languageSwitcher.setLanguage(language, resources) }
    }

    @Test
    fun `Verify refresh app language calls apply current language from language switcher`()= runBlocking {
        val activity = mockk<Activity>(relaxed = true)
        testSubject.refreshAppLanguage(activity)
        coVerify { languageSwitcher.applyCurrentLanguage(activity) }
    }
}