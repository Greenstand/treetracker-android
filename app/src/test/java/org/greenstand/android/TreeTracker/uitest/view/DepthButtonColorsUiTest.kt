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

import androidx.compose.ui.graphics.Color
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DepthButtonColorsUiTest : ComposeUiTest() {
    private val colors =
        DepthButtonColors(
            shadowColor = Color.Red,
            color = Color.Green,
            disabledShadowColor = Color.Blue,
            disabledColor = Color.Yellow,
        )

    @Test
    fun `enabled colors use the shadow and main colors`() {
        var background: Color? = null
        var content: Color? = null
        setContent {
            background = colors.backgroundColor(enabled = true).value
            content = colors.contentColor(enabled = true).value
        }

        composeTestRule.runOnIdle {
            assertEquals(Color.Red, background)
            assertEquals(Color.Green, content)
        }
    }

    @Test
    fun `disabled colors use the disabled shadow and disabled colors`() {
        var background: Color? = null
        var content: Color? = null
        setContent {
            background = colors.backgroundColor(enabled = false).value
            content = colors.contentColor(enabled = false).value
        }

        composeTestRule.runOnIdle {
            assertEquals(Color.Blue, background)
            assertEquals(Color.Yellow, content)
        }
    }

    @Test
    fun `colors with the same values are equal and share a hash code`() {
        val same =
            DepthButtonColors(
                shadowColor = Color.Red,
                color = Color.Green,
                disabledShadowColor = Color.Blue,
                disabledColor = Color.Yellow,
            )

        assertEquals(colors, same)
        assertEquals(colors.hashCode(), same.hashCode())
    }

    @Test
    fun `colors differing in any value are not equal`() {
        assertNotEquals(colors, DepthButtonColors(Color.Black, Color.Green, Color.Blue, Color.Yellow))
        assertNotEquals(colors, DepthButtonColors(Color.Red, Color.Black, Color.Blue, Color.Yellow))
        assertNotEquals(colors, DepthButtonColors(Color.Red, Color.Green, Color.Black, Color.Yellow))
        assertNotEquals(colors, DepthButtonColors(Color.Red, Color.Green, Color.Blue, Color.Black))
    }

    @Test
    fun `colors are not equal to null or another type`() {
        assertNotEquals(colors, null)
        assertNotEquals(colors, Color.Red)
    }
}