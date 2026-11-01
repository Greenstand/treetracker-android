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
package org.greenstand.android.TreeTracker.uitest.view

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.RoundedLocalImageContainer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Tests for [LocalImage], which decodes a photo from disk and falls back to a placeholder.
 * Native graphics mode gives real bitmap decoding.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImagesUiTest : ComposeUiTest() {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun photoFile(): File {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val file = tempFolder.newFile("photo.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    @Test
    fun `an existing photo is shown instead of the placeholder`() {
        val path = photoFile().absolutePath
        setContent {
            LocalImage(
                imagePath = path,
                contentDescription = PHOTO,
                contentScale = ContentScale.Crop,
                placeHolder = { Text(PLACEHOLDER) },
            )
        }

        composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
            composeTestRule.onAllNodes(hasContentDescription(PHOTO)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(PHOTO).assertExists()
        composeTestRule.onNodeWithText(PLACEHOLDER).assertDoesNotExist()
    }

    @Test
    fun `a missing photo shows the placeholder`() {
        setContent {
            LocalImage(
                imagePath = "/no/such/photo.jpg",
                contentDescription = PHOTO,
                contentScale = ContentScale.Crop,
                placeHolder = { Text(PLACEHOLDER) },
            )
        }

        composeTestRule.onNodeWithText(PLACEHOLDER).assertExists()
        composeTestRule.onNodeWithContentDescription(PHOTO).assertDoesNotExist()
    }

    @Test
    fun `an empty path shows the placeholder`() {
        setContent {
            LocalImage(
                imagePath = "",
                contentDescription = PHOTO,
                contentScale = ContentScale.Crop,
                placeHolder = { Text(PLACEHOLDER) },
            )
        }

        composeTestRule.onNodeWithText(PLACEHOLDER).assertExists()
        composeTestRule.onNodeWithContentDescription(PHOTO).assertDoesNotExist()
    }

    @Test
    fun `the default placeholder keeps the requested size`() {
        setContent {
            LocalImage(
                modifier = Modifier.size(48.dp).testTag(TAG),
                imagePath = "/no/such/photo.jpg",
                contentScale = ContentScale.Crop,
            )
        }

        composeTestRule
            .onNodeWithTag(TAG)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `rounded local image container is a 70dp square even without a photo`() {
        setContent {
            Box {
                RoundedLocalImageContainer(
                    modifier = Modifier.testTag(TAG),
                    imagePath = "/no/such/photo.jpg",
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TAG)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(70.dp)
    }

    private companion object {
        const val PHOTO = "Planter photo"
        const val PLACEHOLDER = "No photo"
        const val TAG = "image"
        const val WAIT_TIMEOUT_MS = 5_000L
    }
}