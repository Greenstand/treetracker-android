package org.greenstand.android.TreeTracker.utils

import kotlinx.coroutines.flow.MutableStateFlow

fun <T> MutableStateFlow<T>.updateState(onUpdate: T.() -> T) {
    value = value.onUpdate()
}