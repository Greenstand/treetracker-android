package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.ExperimentalComposeApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.root.Root
import org.koin.android.ext.android.inject
import timber.log.Timber

class TreeTrackerActivity : ComponentActivity() {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()
    private val messagesRepo: MessagesRepo by inject()

    @ExperimentalComposeApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            Timber.d("JONATHAN START")
            messagesRepo.getMessages("admin")
                .forEach {
                    Timber.d("JONATHAN $it")
                }
        }
        if (FeatureFlags.USE_SWAHILI) {
            languageSwitcher.setLanguage(Language.SWAHILI, resources)
        } else {
            languageSwitcher.applyCurrentLanguage(this)
        }

        setContent {
            Root(viewModelFactory)
        }
    }
}