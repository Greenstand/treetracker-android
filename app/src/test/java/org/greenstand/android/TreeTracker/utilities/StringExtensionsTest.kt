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
package org.greenstand.android.TreeTracker.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.NoSuchAlgorithmException

class StringExtensionsTest {
    @Test
    fun `md5 returns the lowercase hex digest`() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", "hello".md5())
    }

    @Test
    fun `md5 pads bytes below 0x10 with a leading zero`() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", "".md5())
    }

    @Test
    fun `md5 hashes the utf-8 bytes`() {
        assertEquals("33fcd01258e6ba07ef405dc9e721f3a7", "hé".md5())
    }

    @Test
    fun `md5 is always 32 hex characters`() {
        listOf("", "a", "Treetracker", "a much longer string with spaces and 123").forEach {
            val hash = it.md5()
            assertEquals(32, hash.length)
            assertEquals(hash, hash.filter { char -> char in "0123456789abcdef" })
        }
    }

    @Test
    fun `hashString returns the uppercase hex digest`() {
        assertEquals(
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
            "abc".hashString("SHA-256"),
        )
        assertEquals("AAF4C61DDCC5E8A2DABEDE0F3B482CD9AEA9434D", "hello".hashString("SHA-1"))
    }

    @Test
    fun `hashString with MD5 matches md5 ignoring case`() {
        assertEquals("hello".md5().uppercase(), "hello".hashString("MD5"))
        assertEquals("".md5().uppercase(), "".hashString("MD5"))
    }

    @Test(expected = NoSuchAlgorithmException::class)
    fun `hashString with an unknown algorithm throws`() {
        "abc".hashString("NOT-AN-ALGORITHM")
    }
}