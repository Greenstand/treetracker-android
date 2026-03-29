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
package org.greenstand.android.TreeTracker.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ApprovalButton
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.CaptureButton
import org.greenstand.android.TreeTracker.view.InfoButton
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.junit.Test

class ButtonScreenshotTest : ScreenshotTest() {

    @Test
    fun arrowButton_left() =
        snapshot {
            ButtonShowcase {
                Box(modifier = Modifier.size(80.dp)) {
                    ArrowButton(isLeft = true, onClick = {})
                }
            }
        }

    @Test
    fun arrowButton_right() =
        snapshot {
            ButtonShowcase {
                Box(modifier = Modifier.size(80.dp)) {
                    ArrowButton(isLeft = false, onClick = {})
                }
            }
        }

    @Test
    fun arrowButton_disabled() =
        snapshot {
            ButtonShowcase {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(80.dp)) {
                        ArrowButton(isLeft = true, isEnabled = false, onClick = {})
                    }
                    Box(modifier = Modifier.size(80.dp)) {
                        ArrowButton(isLeft = false, isEnabled = false, onClick = {})
                    }
                }
            }
        }

    @Test
    fun captureButton() =
        snapshot {
            ButtonShowcase {
                CaptureButton(onClick = {}, isEnabled = true)
            }
        }

    @Test
    fun approvalButton_approve() =
        snapshot {
            ButtonShowcase {
                ApprovalButton(onClick = {}, approval = true)
            }
        }

    @Test
    fun approvalButton_reject() =
        snapshot {
            ButtonShowcase {
                ApprovalButton(onClick = {}, approval = false)
            }
        }

    @Test
    fun languageButton() =
        snapshot {
            ButtonShowcase {
                Box(modifier = Modifier.size(120.dp, 80.dp)) {
                    LanguageButton()
                }
            }
        }

    @Test
    fun infoButton() =
        snapshot {
            ButtonShowcase {
                InfoButton(onClick = {})
            }
        }

    @Test
    fun orangeAddButton() =
        snapshot {
            ButtonShowcase {
                OrangeAddButton(modifier = Modifier, onClick = {})
            }
        }

    @Test
    fun selectionButton_selected_and_unselected() =
        snapshot {
            ButtonShowcase {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TreeTrackerButton(
                        modifier = Modifier.size(width = 120.dp, height = 50.dp),
                        colors = AppButtonColors.ProgressGreen,
                        isSelected = false,
                        onClick = {},
                    ) {
                        Text(
                            "Normal",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                        )
                    }
                    TreeTrackerButton(
                        modifier = Modifier.size(width = 120.dp, height = 50.dp),
                        colors = AppButtonColors.ProgressGreen,
                        isSelected = true,
                        onClick = {},
                    ) {
                        Text(
                            "Selected",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                        )
                    }
                }
            }
        }
}

@Composable
private fun ButtonShowcase(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AppColors.Gray)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}