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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.map.MapContent
import org.greenstand.android.TreeTracker.map.MapMarker
import org.greenstand.android.TreeTracker.map.MapState
import org.greenstand.android.TreeTracker.map.TreeMarkerCard
import org.greenstand.android.TreeTracker.map.TreeMarkerCarousel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for [TreeMarkerCard] and [TreeMarkerCarousel] on their own, and the carousel reacting to [MapContent] state. */
class TreeMarkerCarouselUiTest : ComposeUiTest() {
    private val plantDate = Instant.parse("2024-03-15T12:00:00Z")

    /** Ten markers whose latitude is their index, so each card has a unique "Lat: N.000000" line. */
    private val manyMarkers = (0 until 10).map { marker(index = it) }

    private fun marker(
        index: Int,
        note: String = "",
    ) = MapMarker(
        latitude = index.toDouble(),
        longitude = 30.0,
        id = "tree_$index",
        isUploaded = false,
        note = note,
        plantDate = plantDate,
        imagePath = null,
    )

    private fun latText(index: Int) = "Lat: $index.000000"

    @Test
    fun `card rounds coordinates to six decimals`() {
        val precise = marker(0).copy(latitude = 12.34567891, longitude = -0.0000004)
        setContent { TreeMarkerCard(marker = precise, isSelected = false, onClick = {}) }

        composeTestRule.onNodeWithText("Lat: 12.345679").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lng: -0.000000").assertIsDisplayed()
    }

    @Test
    fun `card shows the tree image with a placeholder when the tree has no photo`() {
        setContent { TreeMarkerCard(marker = marker(0), isSelected = false, onClick = {}) }

        composeTestRule.onNodeWithContentDescription(TREE_IMAGE_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun `unselected card uses the normal size`() {
        setContent {
            TreeMarkerCard(marker = marker(0), isSelected = false, onClick = {}, modifier = Modifier.testTag(CARD_TAG))
        }

        composeTestRule
            .onNodeWithTag(CARD_TAG)
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(280.dp)
    }

    @Test
    fun `selected card is ten percent larger`() {
        setContent {
            TreeMarkerCard(marker = marker(0), isSelected = true, onClick = {}, modifier = Modifier.testTag(CARD_TAG))
        }

        composeTestRule
            .onNodeWithTag(CARD_TAG)
            .assertWidthIsEqualTo(220.dp)
            .assertHeightIsEqualTo(308.dp)
    }

    @Test
    fun `card grows when it becomes selected`() {
        var isSelected by mutableStateOf(false)
        setContent {
            TreeMarkerCard(marker = marker(0), isSelected = isSelected, onClick = {}, modifier = Modifier.testTag(CARD_TAG))
        }
        composeTestRule.onNodeWithTag(CARD_TAG).assertWidthIsEqualTo(200.dp)

        isSelected = true

        composeTestRule.onNodeWithTag(CARD_TAG).assertWidthIsEqualTo(220.dp)
    }

    @Test
    fun `clicking a card reports a click`() {
        var clicks = 0
        setContent { TreeMarkerCard(marker = marker(0, note = "Avocado"), isSelected = false, onClick = { clicks++ }) }

        composeTestRule.onNodeWithText("Avocado").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `carousel reports the whole clicked marker`() {
        val clicked = mutableListOf<MapMarker>()
        setContent {
            TreeMarkerCarousel(markers = manyMarkers.take(2), selectedMarkerId = null, onMarkerClick = { clicked += it })
        }

        composeTestRule.onNodeWithText(latText(1)).performClick()

        assertEquals(listOf(manyMarkers[1]), clicked)
    }

    @Test
    fun `carousel starts at the first tree when nothing is selected`() {
        setContent { TreeMarkerCarousel(markers = manyMarkers, selectedMarkerId = null, onMarkerClick = {}) }

        composeTestRule.onNodeWithText(latText(0)).assertIsDisplayed()
        composeTestRule.onNodeWithText(latText(9)).assertDoesNotExist()
    }

    @Test
    fun `carousel opens scrolled to a tree selected from the start`() {
        setContent { TreeMarkerCarousel(markers = manyMarkers, selectedMarkerId = "tree_8", onMarkerClick = {}) }

        composeTestRule.onNodeWithText(latText(8)).assertIsDisplayed()
        composeTestRule.onNodeWithText(latText(0)).assertDoesNotExist()
    }

    @Test
    fun `carousel scrolls to a tree when it is selected later`() {
        var selectedId by mutableStateOf<String?>(null)
        setContent { TreeMarkerCarousel(markers = manyMarkers, selectedMarkerId = selectedId, onMarkerClick = {}) }
        composeTestRule.onNodeWithText(latText(7)).assertDoesNotExist()

        selectedId = "tree_7"

        composeTestRule.onNodeWithText(latText(7)).assertIsDisplayed()
    }

    @Test
    fun `carousel stays put when the selected id is not one of its trees`() {
        setContent { TreeMarkerCarousel(markers = manyMarkers, selectedMarkerId = "tree_404", onMarkerClick = {}) }

        composeTestRule.onNodeWithText(latText(0)).assertIsDisplayed()
    }

    @Test
    fun `map content shows the carousel once trees load and hides it when they are gone`() {
        var state by mutableStateOf(MapState())
        setContent {
            MapContent(state = state, onBackClicked = {}, onMarkerSelected = {}, map = { Box(Modifier.fillMaxSize()) })
        }
        composeTestRule.onNodeWithContentDescription(TREE_IMAGE_DESCRIPTION).assertDoesNotExist()

        state = MapState(markers = listOf(marker(3)))
        composeTestRule.onNodeWithText(latText(3)).assertIsDisplayed()

        state = MapState(markers = emptyList())
        composeTestRule.onNodeWithText(latText(3)).assertDoesNotExist()
    }

    @Test
    fun `selecting a marker on the map scrolls the carousel to its card`() {
        var state by mutableStateOf(MapState(markers = manyMarkers))
        setContent {
            MapContent(state = state, onBackClicked = {}, onMarkerSelected = {}, map = { Box(Modifier.fillMaxSize()) })
        }

        state = state.copy(selectedMarkerId = "tree_9")

        composeTestRule.onNodeWithText(latText(9)).assertIsDisplayed()
    }

    private companion object {
        const val CARD_TAG = "TreeMarkerCard"
        const val TREE_IMAGE_DESCRIPTION = "Tree image"
    }
}