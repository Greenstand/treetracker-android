package org.greenstand.android.TreeTracker.orgpicker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.greenstand.android.TreeTracker.activities.TreeTrackerActivity
import org.greenstand.android.TreeTracker.models.organization.Org
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.compose.viewModel

@RunWith(AndroidJUnit4::class)
class OrgPickerScreenKtTest {

    @get:Rule
    val rule = createAndroidComposeRule<TreeTrackerActivity>()

    @Test
    fun OrgItemTest() {

        rule.setContent {
            OrgItem(
                Org(
                    FakeOrgITem.orgitem.id,
                    FakeOrgITem.orgitem.name,
                    FakeOrgITem.orgitem.walletId,
                    FakeOrgITem.orgitem.logoPath,
                    FakeOrgITem.orgitem.captureSetupFlow,
                    FakeOrgITem.orgitem.captureFlow
                ), isSelected = false, onClick ={})



        }


        rule.onNode(hasText(FakeOrgITem.orgitem.name), useUnmergedTree = true).assertIsDisplayed()


    }

}