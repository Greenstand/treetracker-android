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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * `rememberNavBackStack` saves the back stack through each route's kotlinx serializer, so every
 * route has to survive a round trip for the app to restore the stack after process death.
 */
class RoutesSerializationTest {
    private val json = Json

    private val noArgRoutes: List<NavKey> =
        listOf(
            SignupFlowRoute,
            DeleteProfileRoute,
            OrgRoute,
            DashboardRoute,
            TreeHeightScreenRoute,
            UserSelectRoute,
            ProfileSelectRoute,
            WalletSelectRoute,
            AddWalletRoute,
            SettingsRoute,
            SessionNoteRoute,
            AddOrgRoute,
            SelfieRoute,
            MessagesUserSelectRoute,
            DevOptionsRoute,
            MapRoute,
            TreeEditUserSelectRoute,
        )

    private val argRoutes: List<NavKey> =
        listOf(
            SplashRoute(orgId = "109288091", orgName = "Kasiki Hai"),
            SplashRoute(),
            ProfileRoute(planterInfoId = 42L),
            IndividualMessageListRoute(planterInfoId = 7L),
            SurveyRoute(messageId = "survey-1"),
            ImageReviewRoute(photoPath = "/data/images/tree 1.jpg"),
            LanguageRoute(isFromTopBar = false),
            TreeCaptureRoute(profilePicUrl = "file:///selfie.jpg"),
            TreeImageReviewRoute(photoPath = "/data/images/tree.jpg"),
            ChatRoute(planterInfoId = Long.MAX_VALUE, otherChatIdentifier = "wallet/with:odd?chars"),
            AnnouncementRoute(messageId = "announcement-1"),
            TreeListRoute(userWallet = "wallet", userName = "Ünïcødé Name"),
            TreeDetailRoute(treeId = -1L),
        )

    @Suppress("UNCHECKED_CAST")
    private fun serializerFor(route: NavKey): KSerializer<NavKey> = serializer(route::class.java) as KSerializer<NavKey>

    private fun roundTrip(route: NavKey): NavKey {
        val serializer = serializerFor(route)
        return json.decodeFromString(serializer, json.encodeToString(serializer, route))
    }

    @Test
    fun `every no-arg route round trips to the same object`() {
        noArgRoutes.forEach { route ->
            assertSame(route, roundTrip(route), "$route did not round trip")
        }
    }

    @Test
    fun `every route with arguments round trips with its arguments intact`() {
        argRoutes.forEach { route ->
            assertEquals(route, roundTrip(route), "$route did not round trip")
        }
    }

    @Test
    fun `route arguments are saved under their property names`() {
        val encoded =
            json
                .parseToJsonElement(
                    json.encodeToString(serializerFor(ChatRoute(1L, "other")), ChatRoute(1L, "other")),
                ).jsonObject

        assertEquals("1", encoded.getValue("planterInfoId").jsonPrimitive.content)
        assertEquals("other", encoded.getValue("otherChatIdentifier").jsonPrimitive.content)
    }

    @Test
    fun `routes with default arguments restore their defaults when saved without them`() {
        assertEquals(SplashRoute(), json.decodeFromString(serializerFor(SplashRoute()), "{}"))
        assertEquals(LanguageRoute(), json.decodeFromString(serializerFor(LanguageRoute()), "{}"))
    }

    @Test
    fun `route names used for screen tracking drop the arguments`() {
        // ScreenTrackingNavEntryDecorator reports toString() up to the first '('.
        assertEquals("DashboardRoute", DashboardRoute.toString().substringBefore('('))
        assertEquals("TreeDetailRoute", TreeDetailRoute(5L).toString().substringBefore('('))
        assertEquals("ChatRoute", ChatRoute(1L, "x(y)").toString().substringBefore('('))
    }
}