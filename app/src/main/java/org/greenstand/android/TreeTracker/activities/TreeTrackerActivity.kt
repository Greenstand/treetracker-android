package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.ExperimentalComposeApi
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.koin.android.ext.android.inject

class TreeTrackerActivity : ComponentActivity() {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()


    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        languageSwitcher.applyCurrentLanguage(this)

        setContent {
            CustomTheme {
                Root(viewModelFactory)
            }
        }
    }
}