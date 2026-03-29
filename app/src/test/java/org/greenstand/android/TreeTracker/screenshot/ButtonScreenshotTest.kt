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
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ApprovalButton
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.CaptureButton
import org.greenstand.android.TreeTracker.view.InfoButton
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButtonShape
import org.junit.Test

class ButtonScreenshotTest : ScreenshotTest() {
    // ── Arrow Buttons ──────────────────────────────────────────────

    @Test
    fun arrowButton_left() =
        snapshot {
            ButtonShowcase("Arrow Left") {
                Box(modifier = Modifier.size(80.dp)) {
                    ArrowButton(isLeft = true, onClick = {})
                }
            }
        }

    @Test
    fun arrowButton_right() =
        snapshot {
            ButtonShowcase("Arrow Right") {
                Box(modifier = Modifier.size(80.dp)) {
                    ArrowButton(isLeft = false, onClick = {})
                }
            }
        }

    @Test
    fun arrowButton_disabled() =
        snapshot {
            ButtonShowcase("Arrow Disabled") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.size(80.dp)) {
                        ArrowButton(
                            isLeft = true,
                            isEnabled = false,
                            onClick = {},
                        )
                    }
                    Box(modifier = Modifier.size(80.dp)) {
                        ArrowButton(
                            isLeft = false,
                            isEnabled = false,
                            onClick = {},
                        )
                    }
                }
            }
        }

    @Test
    fun arrowButton_purple() =
        snapshot {
            ButtonShowcase("Arrow Purple") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.size(80.dp)) {
                        ArrowButton(
                            isLeft = true,
                            colors = AppButtonColors.MessagePurple,
                            onClick = {},
                        )
                    }
                    Box(modifier = Modifier.size(80.dp)) {
                        ArrowButton(
                            isLeft = false,
                            colors = AppButtonColors.MessagePurple,
                            onClick = {},
                        )
                    }
                }
            }
        }

    // ── Approval Buttons ───────────────────────────────────────────

    @Test
    fun approvalButton_approve() =
        snapshot {
            ButtonShowcase("Approval - Accept") {
                ApprovalButton(onClick = {}, approval = true)
            }
        }

    @Test
    fun approvalButton_reject() =
        snapshot {
            ButtonShowcase("Approval - Reject") {
                ApprovalButton(onClick = {}, approval = false)
            }
        }

    @Test
    fun approvalButton_pair() =
        snapshot {
            ButtonShowcase("Approval Pair") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ApprovalButton(onClick = {}, approval = false)
                    ApprovalButton(onClick = {}, approval = true)
                }
            }
        }

    // ── Capture Button ─────────────────────────────────────────────

    @Test
    fun captureButton_enabled() =
        snapshot {
            ButtonShowcase("Capture Enabled") {
                CaptureButton(onClick = {}, isEnabled = true)
            }
        }

    @Test
    fun captureButton_disabled() =
        snapshot {
            ButtonShowcase("Capture Disabled") {
                CaptureButton(onClick = {}, isEnabled = false)
            }
        }

    // ── Orange Add Button ──────────────────────────────────────────

    @Test
    fun orangeAddButton_default() =
        snapshot {
            ButtonShowcase("Orange Add") {
                OrangeAddButton(
                    modifier = Modifier,
                    onClick = {},
                )
            }
        }

    // ── Info Button ────────────────────────────────────────────────

    @Test
    fun infoButton_default() =
        snapshot {
            ButtonShowcase("Info") {
                InfoButton(onClick = {})
            }
        }

    // ── Language Button ────────────────────────────────────────────

    @Test
    fun languageButton_default() =
        snapshot {
            ButtonShowcase("Language") {
                Box(modifier = Modifier.size(120.dp, 80.dp)) {
                    LanguageButton()
                }
            }
        }

    // ── TreeTrackerButton (Base) ───────────────────────────────────

    @Test
    fun treeTrackerButton_rectangle() =
        snapshot {
            ButtonShowcase("Rectangle Button") {
                TreeTrackerButton(
                    modifier = Modifier.size(width = 160.dp, height = 60.dp),
                    onClick = {},
                ) {
                    Text(
                        "Continue",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
                }
            }
        }

    @Test
    fun treeTrackerButton_circle() =
        snapshot {
            ButtonShowcase("Circle Button") {
                TreeTrackerButton(
                    modifier = Modifier.size(80.dp),
                    shape = TreeTrackerButtonShape.Circle,
                    onClick = {},
                ) {
                    Text(
                        "Go",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
                }
            }
        }

    @Test
    fun treeTrackerButton_colors() =
        snapshot {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Gray)
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                listOf(
                    "Green" to AppButtonColors.ProgressGreen,
                    "Purple" to AppButtonColors.MessagePurple,
                    "Red" to AppButtonColors.DeclineRed,
                    "Orange" to AppButtonColors.UploadOrange,
                    "Yellow" to AppButtonColors.Yellow,
                    "SkyBlue" to AppButtonColors.SkyBlue,
                    "White" to AppButtonColors.WhiteLight,
                    "Default" to AppButtonColors.Default,
                ).forEach { (label, colors) ->
                    TreeTrackerButton(
                        modifier = Modifier.size(width = 160.dp, height = 50.dp),
                        colors = colors,
                        onClick = {},
                    ) {
                        Text(
                            label,
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

    @Test
    fun treeTrackerButton_disabled() =
        snapshot {
            ButtonShowcase("Disabled Buttons") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TreeTrackerButton(
                        modifier = Modifier.size(width = 160.dp, height = 50.dp),
                        colors = AppButtonColors.ProgressGreen,
                        isEnabled = false,
                        onClick = {},
                    ) {
                        Text(
                            "Disabled Green",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                        )
                    }
                    TreeTrackerButton(
                        modifier = Modifier.size(width = 160.dp, height = 50.dp),
                        colors = AppButtonColors.MessagePurple,
                        isEnabled = false,
                        onClick = {},
                    ) {
                        Text(
                            "Disabled Purple",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                        )
                    }
                }
            }
        }

    @Test
    fun treeTrackerButton_selected() =
        snapshot {
            ButtonShowcase("Selected vs Unselected") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
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

    // ── ActionBar Compositions ─────────────────────────────────────

    @Test
    fun actionBar_arrows_only() =
        snapshot {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Gray),
                verticalArrangement = Arrangement.Center,
            ) {
                ActionBar(
                    leftAction = {
                        ArrowButton(isLeft = true, onClick = {})
                    },
                    rightAction = {
                        ArrowButton(isLeft = false, onClick = {})
                    },
                )
            }
        }

    @Test
    fun actionBar_with_language() =
        snapshot {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Gray),
                verticalArrangement = Arrangement.Center,
            ) {
                ActionBar(
                    leftAction = {
                        ArrowButton(isLeft = true, onClick = {})
                    },
                    centerAction = {
                        LanguageButton()
                    },
                    rightAction = {
                        ArrowButton(isLeft = false, onClick = {})
                    },
                )
            }
        }

    @Test
    fun actionBar_capture_layout() =
        snapshot {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Gray),
                verticalArrangement = Arrangement.Center,
            ) {
                ActionBar(
                    leftAction = {
                        ArrowButton(isLeft = true, onClick = {})
                    },
                    centerAction = {
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            CaptureButton(onClick = {}, isEnabled = true)
                        }
                    },
                    rightAction = {
                        InfoButton(onClick = {})
                    },
                )
            }
        }

    @Test
    fun actionBar_approval_layout() =
        snapshot {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Gray),
                verticalArrangement = Arrangement.Center,
            ) {
                ActionBar(
                    leftAction = {
                        ApprovalButton(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = {},
                            approval = false,
                        )
                    },
                    rightAction = {
                        ApprovalButton(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = {},
                            approval = true,
                        )
                    },
                )
            }
        }
}

/**
 * Helper composable that centers a button component with a label
 * on a dark background for consistent screenshot framing.
 */
@Composable
private fun ButtonShowcase(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AppColors.Gray)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}