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

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController

@OptIn(ExperimentalUnitApi::class)
object TextStyles {

    val DarkText = TextStyle(
        color = AppColors.GrayShadow,
        fontSize = TextUnit(24f, TextUnitType.Sp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun BoxScope.TopBarTitle() {
    val nav = LocalNavHostController.current
    Image(
        painter = painterResource(id = R.drawable.greenstand_logo),
        contentDescription = "Treetracker icon",
        modifier = Modifier
            .height(100.dp)
            .width(100.dp)
            .align(Alignment.Center)
            .padding(all = 15.dp).run {
                if (BuildConfig.DEBUG) {
                    this.pointerInput(true) {
                        detectTapGestures(
                            onLongPress = {
                                nav.navigate(NavRoute.DevOptions.route)
                            }
                        )
                    }
                } else {
                    this
                }
            }
    )
}