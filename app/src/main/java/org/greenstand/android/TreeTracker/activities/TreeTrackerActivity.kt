package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.databinding.TreeTrackerActivityBinding
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.koin.android.ext.android.inject

class TreeTrackerActivity : AppCompatActivity() {

    lateinit var bindings: TreeTrackerActivityBinding

    private val languageSwitcher: LanguageSwitcher by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FeatureFlags.USE_SWAHILI) {
            languageSwitcher.setLanguage(Language.SWAHILI, resources)
        } else {
            languageSwitcher.applyCurrentLanguage(this)
        }

        bindings = TreeTrackerActivityBinding.inflate(layoutInflater)

        setContentView(bindings.root)
    }
}