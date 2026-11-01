/*
 * Copyright 2026 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.uitest.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.greenstand.android.TreeTracker.map.MapContent
import org.greenstand.android.TreeTracker.map.MapMarker
import org.greenstand.android.TreeTracker.map.MapState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.format.DateTimeFormatter

/** Tests for the stateless [MapContent] overlay, with a placeholder instead of MapLibre. */
class MapUiTest : ComposeUiTest() {
    private var backClicks = 0
    private val selectedMarkerIds = mutableListOf<String>()

    private val plantDate = Instant.parse("2024-03-15T12:00:00Z")

    private val notedMarker =
        MapMarker(
            latitude = -1.2921,
            longitude = 36.8219,
            id = "tree_1",
            isUploaded = true,
            note = "Mango by the gate",
            plantDate = plantDate,
            imagePath = null,
            sessionId = 4L,
        )

    private val unnotedMarker =
        MapMarker(
            latitude = 0.5,
            longitude = 35.25,
            id = "tree_2",
            isUploaded = false,
            note = "",
            plantDate = plantDate,
            imagePath = "/no/such/tree.jpg",
        )

    private fun render(state: MapState) {
        setContent {
            MapContent(
                state = state,
                onBackClicked = { backClicks++ },
                onMarkerSelected = { selectedMarkerIds += it },
                map = { Box(Modifier.fillMaxSize().testTag(MAP_TAG)) },
            )
        }
    }

    @Test
    fun `map and back button are shown without a carousel when there are no trees`() {
        render(MapState())

        composeTestRule.onNodeWithTag(MAP_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(TREE_IMAGE_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun `back button reports a back click`() {
        render(MapState())

        composeTestRule.onNodeWithContentDescription(BACK_DESCRIPTION).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `carousel shows a card per tree with its location note and plant date`() {
        render(MapState(markers = listOf(notedMarker, unnotedMarker), isLoading = false))

        composeTestRule.onAllNodesWithContentDescription(TREE_IMAGE_DESCRIPTION).assertCountEquals(2)
        composeTestRule.onNodeWithText("Lat: -1.292100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lng: 36.821900").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mango by the gate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lat: 0.500000").assertIsDisplayed()
        composeTestRule.onNodeWithText("No note").assertIsDisplayed()
    }

    @Test
    fun `cards show the plant date in the device time zone`() {
        render(MapState(markers = listOf(notedMarker)))

        val expectedDate =
            plantDate
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        composeTestRule.onNodeWithText(expectedDate).assertIsDisplayed()
    }

    @Test
    fun `clicking a card selects its marker`() {
        render(MapState(markers = listOf(notedMarker, unnotedMarker)))

        composeTestRule.onNodeWithText("Lat: 0.500000").performClick()

        assertEquals(listOf(unnotedMarker.id), selectedMarkerIds)
    }

    @Test
    fun `selected card is wider than the others`() {
        render(MapState(markers = listOf(notedMarker, unnotedMarker), selectedMarkerId = notedMarker.id))

        composeTestRule.onNodeWithText("Lat: -1.292100").assertWidthIsEqualTo(220.dp)
        composeTestRule.onNodeWithText("Lat: 0.500000").assertWidthIsEqualTo(200.dp)
    }

    private companion object {
        const val MAP_TAG = "Map"
        const val BACK_DESCRIPTION = "Back"
        const val TREE_IMAGE_DESCRIPTION = "Tree image"
    }
}