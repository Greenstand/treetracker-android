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
package org.greenstand.android.TreeTracker.map

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AppColors
import org.maplibre.android.MapLibre
import java.io.File
import java.time.format.DateTimeFormatter

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val navController = LocalNavHostController.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LibreMap(
            markers = state.markers,
            selectedMarkerId = state.selectedMarkerId,
            styleUrl = "https://tiles.openfreemap.org/styles/liberty",
            onMarkerClick = { markerId ->
                viewModel.selectMarker(markerId)
            }
        )

        // Back button in top left corner
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 4.dp)
                .statusBarsPadding(),
            elevation = 4.dp,
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }

        // Carousel overlay at bottom
        if (state.markers.isNotEmpty()) {
            TreeMarkerCarousel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                markers = state.markers,
                selectedMarkerId = state.selectedMarkerId,
                onMarkerClick = { marker ->
                    viewModel.selectMarker(marker.id)
                }
            )
        }
    }
}

@Composable
fun TreeMarkerCarousel(
    markers: List<MapMarker>,
    selectedMarkerId: String?,
    onMarkerClick: (MapMarker) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calculate centering offset based on selected card size
    val screenWidth = configuration.screenWidthDp.dp
    val centerOffset = with(density) {
        ((screenWidth - CARD_WIDTH_SELECTED) / 2).toPx().toInt()
    }

    // Scroll to selected marker when it changes
    LaunchedEffect(selectedMarkerId) {
        selectedMarkerId?.let { id ->
            val index = markers.indexOfFirst { it.id == id }
            if (index != -1) {
                coroutineScope.launch {
                    // Scroll with offset to center the selected item
                    listState.animateScrollToItem(
                        index = index,
                        scrollOffset = -centerOffset
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = modifier.height(CARD_HEIGHT_SELECTED),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(markers, key = { it.id }) { marker ->
            TreeMarkerCard(
                marker = marker,
                isSelected = marker.id == selectedMarkerId,
                onClick = { onMarkerClick(marker) }
            )
        }
    }
}

@Composable
fun TreeMarkerCard(
    marker: MapMarker,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate width and height by 10% when selected
    val cardWidth by animateDpAsState(
        targetValue = if (isSelected) CARD_WIDTH_SELECTED else CARD_WIDTH_NORMAL,
        animationSpec = tween(durationMillis = 300),
        label = "card_width"
    )

    val cardHeight by animateDpAsState(
        targetValue = if (isSelected) CARD_HEIGHT_SELECTED else CARD_HEIGHT_NORMAL,
        animationSpec = tween(durationMillis = 300),
        label = "card_height"
    )

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clickable { onClick() },
        elevation = if (isSelected) 8.dp else 4.dp,
        backgroundColor = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // Tree image loaded from file path - takes 2/3 of card space
            AsyncImage(
                model = marker.imagePath?.let { File(it) },
                contentDescription = "Tree image",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.yellow_leafs_placeholder),
                error = painterResource(id = R.drawable.yellow_leafs_placeholder),
                fallback = painterResource(id = R.drawable.yellow_leafs_placeholder)
            )

            // Content section - takes 1/3 of card space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Lat: ${String.format("%.6f", marker.latitude)}",
                    style = CustomTheme.typography.small,
                    color = AppColors.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Lng: ${String.format("%.6f", marker.longitude)}",
                    style = CustomTheme.typography.small,
                    color = AppColors.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Note
                Text(
                    text = marker.note.ifEmpty { "No note" },
                    style = CustomTheme.typography.small,
                    color = AppColors.MediumGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Plant date
                Text(
                    text = formatPlantDate(marker.plantDate),
                    style = CustomTheme.typography.small,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Green,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatPlantDate(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val javaDateTime = localDateTime.toJavaLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    return javaDateTime.format(formatter)
}

// Card size constants
private val CARD_WIDTH_NORMAL = 200.dp
private val CARD_WIDTH_SELECTED = 220.dp
private val CARD_HEIGHT_NORMAL = 280.dp
private val CARD_HEIGHT_SELECTED = 308.dp
