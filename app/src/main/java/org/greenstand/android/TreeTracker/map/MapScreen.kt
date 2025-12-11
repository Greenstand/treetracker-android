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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.maplibre.android.MapLibre
import java.time.format.DateTimeFormatter

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val navController = LocalNavHostController.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        MapLibre.getInstance(context)
        onDispose { }
    }

    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.map_title),
                        color = AppColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = CustomTheme.typography.large,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LibreMap(
                markers = state.markers,
                selectedMarkerId = state.selectedMarkerId
            )

            // Carousel at the bottom
            if (state.markers.isNotEmpty()) {
                TreeMarkerCarousel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    markers = state.markers,
                    selectedMarkerId = state.selectedMarkerId,
                    onMarkerClick = { marker ->
                        viewModel.selectMarker(marker.id)
                    }
                )
            }
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

    // Calculate centering offset
    val cardWidth = 200.dp
    val screenWidth = configuration.screenWidthDp.dp
    val centerOffset = with(density) {
        ((screenWidth - cardWidth) / 2).toPx().toInt()
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
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    Card(
        modifier = modifier
            .width(200.dp)
            .clickable { onClick() },
        elevation = if (isSelected) 8.dp else 4.dp,
        backgroundColor = AppColors.LightGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Placeholder image
            Image(
                painter = painterResource(id = R.drawable.yellow_leafs_placeholder),
                contentDescription = "Tree placeholder",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Latitude/Longitude
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
