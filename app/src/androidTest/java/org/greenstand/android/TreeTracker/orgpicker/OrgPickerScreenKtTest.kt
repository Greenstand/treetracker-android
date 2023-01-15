package org.greenstand.android.TreeTracker.orgpicker

import android.content.Context
import android.content.res.Resources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.greenstand.android.TreeTracker.activities.TreeTrackerActivity
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.compose.R
import org.koin.androidx.compose.viewModel

@RunWith(AndroidJUnit4::class)
class OrgPickerScreenKtTest {

    @get:Rule
    val rule = createAndroidComposeRule<TreeTrackerActivity>()




    //Test is working
    @Test
    fun OrgItemNameEqualsInputName() {

        var isClicked:Boolean = false

        rule.setContent {
            OrgItem(
                Org(
                    id = "123",
                    name = "BESTORG",
                    walletId = "abc",
                    logoPath = "path",
                    mutableListOf(
                        Destination(
                        "route",
                        mutableListOf("listofDestination")
                    )
                    ),
                    mutableListOf(
                        Destination(
                        "route",
                        mutableListOf("listOfDestination")
                    )
                    )
                ), isSelected = true,

                onClick ={ isClicked = true
                })



        }

        rule.onNode(hasText("BESTORG"), useUnmergedTree = true).assertIsDisplayed()

    }

    //Test working

    @Test
    fun OrgisClickable() {

        var clicked= false

        rule.setContent {
            OrgItem(
                Org(
                    id = "123",
                    name = "BESTORG",
                    walletId = "ABC",
                    logoPath = "PATH",
                    mutableListOf(
                        Destination(
                            "route",
                            mutableListOf("listofDestination")
                        )
                    ),
                    mutableListOf(
                        Destination(
                            "route",
                            mutableListOf("listOfDestination")
                        )
                    )
                ), isSelected = true,

                onClick ={ clicked = true
                })
        }

        rule.onNode(hasTestTag("TreeTrackerButton")).performClick()
        assertTrue(clicked)

    }


    //Test is not working
    @Test
    fun TopBarTest(){

      rule.setContent {
          OrgPickerScreen()
      }
        rule.onNode(hasText("SELECT ORGANIZATION")).assertIsDisplayed()
    }


}