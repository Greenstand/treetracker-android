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
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A localization-agnostic text reference that ViewModels can emit without holding a
 * [Context]. Resolved to a `String` on the UI side by the consuming Composable.
 */
sealed interface TextRef {
    data class Plain(val value: String) : TextRef

    data class Res(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : TextRef {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    companion object {
        /** Convenience builder so call sites read `TextRef(R.string.foo)` instead of `TextRef.Res(R.string.foo)`. */
        operator fun invoke(
            @StringRes id: Int,
            vararg args: Any,
        ): TextRef = Res(id, args.toList())

        operator fun invoke(value: String): TextRef = Plain(value)
    }
}

fun Context.resolve(text: TextRef): String =
    when (text) {
        is TextRef.Plain -> text.value
        is TextRef.Res ->
            if (text.args.isEmpty()) {
                getString(text.id)
            } else {
                getString(text.id, *text.args.toTypedArray())
            }
    }

@Composable
fun TextRef.asString(): String =
    when (this) {
        is TextRef.Plain -> value
        is TextRef.Res ->
            if (args.isEmpty()) stringResource(id) else stringResource(id, *args.toTypedArray())
    }
