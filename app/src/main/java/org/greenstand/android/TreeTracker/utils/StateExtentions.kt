package org.greenstand.android.TreeTracker.utils

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow

inline fun <T> MutableStateFlow<T>.updateState(onUpdate: T.() -> T) {
    value = value.onUpdate()
}

inline fun <T> MutableLiveData<T>.updateState(onUpdate: T.() -> T) {
    postValue(onUpdate(value ?: throw IllegalStateException("Must have state")))
}