/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActionBar(
    modifier: Modifier = Modifier,
    leftAction: @Composable (BoxScope.() -> Unit) = { },
    centerAction: @Composable (BoxScope.() -> Unit) = { },
    rightAction: @Composable (BoxScope.() -> Unit) = { },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            leftAction()
        }
        Box(modifier = Modifier.weight(1f)) {
            centerAction()
        }
        Box(modifier = Modifier.weight(1f)) {
            rightAction()
        }
    }
}