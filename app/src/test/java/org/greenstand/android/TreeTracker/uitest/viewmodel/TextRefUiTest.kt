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
package org.greenstand.android.TreeTracker.uitest.viewmodel

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.onNodeWithText
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.viewmodel.TextRef
import org.greenstand.android.TreeTracker.viewmodel.asString
import org.junit.Test

class TextRefUiTest : ComposeUiTest() {
    @Test
    fun `asString shows a plain value unchanged`() {
        setContent { BasicText(TextRef("Plain value").asString()) }

        composeTestRule.onNodeWithText("Plain value").assertExists()
    }

    @Test
    fun `asString resolves a resource without args`() {
        setContent { BasicText(TextRef(R.string.tree_note_saved).asString()) }

        composeTestRule.onNodeWithText(string(R.string.tree_note_saved)).assertExists()
    }

    @Test
    fun `asString formats a resource with args`() {
        setContent { BasicText(TextRef(R.string.app_version, "1.2.3", 42).asString()) }

        composeTestRule.onNodeWithText(string(R.string.app_version, "1.2.3", 42)).assertExists()
    }

    @Test
    fun `asString formats a resource built from an args list`() {
        setContent { BasicText(TextRef.Res(R.string.tree_list_title, listOf<Any>("Ada")).asString()) }

        composeTestRule.onNodeWithText(string(R.string.tree_list_title, "Ada")).assertExists()
    }
}