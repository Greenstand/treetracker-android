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
package org.greenstand.android.TreeTracker.capture

import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import java.util.UUID

/** Builds a real [CaptureFlowNavigationController] over an org capture flow for ViewModel tests. */
internal object CaptureFlowTestFixtures {
    const val PHOTO_PATH = "/photos/tree.jpg"

    /** capture -> image review -> tree height, then back to capture. */
    val defaultCaptureFlow: List<Destination> =
        listOf(
            Destination(route = RouteRegistry.ROUTE_TREE_CAPTURE),
            Destination(route = RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
            Destination(route = RouteRegistry.ROUTE_TREE_HEIGHT),
        )

    /** capture -> image review, then back to capture. */
    val shortCaptureFlow: List<Destination> =
        listOf(
            Destination(route = RouteRegistry.ROUTE_TREE_CAPTURE),
            Destination(route = RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
        )

    fun tree(photoPath: String = PHOTO_PATH): Tree =
        Tree(
            treeUuid = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            sessionId = 1L,
            content = "",
            photoPath = photoPath,
            meanLongitude = 2.0,
            meanLatitude = 1.0,
        )

    fun controller(
        treeCapturer: TreeCapturer,
        captureFlow: List<Destination> = defaultCaptureFlow,
    ): CaptureFlowNavigationController {
        val orgRepo = mockk<OrgRepo>()
        every { orgRepo.currentOrg() } returns
            Org(
                id = "org-id",
                name = "Org",
                walletId = "wallet",
                logoPath = "",
                captureSetupFlow = emptyList(),
                captureFlow = captureFlow,
            )
        return CaptureFlowNavigationController(
            orgRepo = orgRepo,
            stepCounter = mockk<StepCounter>(relaxed = true),
            sessionTracker = mockk<SessionTracker>(relaxed = true),
            locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true),
            treeCapturer = treeCapturer,
            deviceOrientation = mockk<DeviceOrientation>(relaxed = true),
        )
    }
}