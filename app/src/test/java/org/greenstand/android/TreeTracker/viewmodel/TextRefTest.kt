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
package org.greenstand.android.TreeTracker.viewmodel

import android.content.Context
import org.greenstand.android.TreeTracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TextRefTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    // --- factories ---

    @Test
    fun `invoke with a string builds a Plain ref`() {
        assertEquals(TextRef.Plain("hello"), TextRef("hello"))
    }

    @Test
    fun `invoke with a resource id builds a Res ref without args`() {
        assertEquals(TextRef.Res(R.string.tree_note_saved, emptyList()), TextRef(R.string.tree_note_saved))
    }

    @Test
    fun `invoke with a resource id and args builds a Res ref with those args in order`() {
        val ref = TextRef(R.string.app_version, "1.2.3", 42)

        assertEquals(TextRef.Res(R.string.app_version, listOf<Any>("1.2.3", 42)), ref)
    }

    @Test
    fun `Res vararg constructor collects args into a list`() {
        val ref = TextRef.Res(R.string.app_version, "1.2.3", 42)

        assertEquals(listOf<Any>("1.2.3", 42), ref.args)
    }

    @Test
    fun `Res with only an id has no args`() {
        assertTrue(TextRef.Res(R.string.tree_note_saved).args.isEmpty())
    }

    @Test
    fun `refs with different args are not equal`() {
        assertNotEquals(TextRef(R.string.tree_list_title, "Ada"), TextRef(R.string.tree_list_title, "Bea"))
    }

    // --- Context.resolve ---

    @Test
    fun `resolve returns a Plain value unchanged`() {
        assertEquals("plain text", context.resolve(TextRef("plain text")))
    }

    @Test
    fun `resolve looks up a resource without args`() {
        assertEquals(context.getString(R.string.tree_note_saved), context.resolve(TextRef(R.string.tree_note_saved)))
    }

    @Test
    fun `resolve formats a resource with args`() {
        val resolved = context.resolve(TextRef(R.string.app_version, "1.2.3", 42))

        assertEquals(context.getString(R.string.app_version, "1.2.3", 42), resolved)
        assertTrue(resolved.contains("1.2.3"))
        assertTrue(resolved.contains("42"))
    }

    @Test
    fun `resolve formats a resource built from an explicit args list`() {
        val resolved = context.resolve(TextRef.Res(R.string.tree_list_title, listOf<Any>("Ada")))

        assertEquals(context.getString(R.string.tree_list_title, "Ada"), resolved)
        assertTrue(resolved.contains("Ada"))
    }
}