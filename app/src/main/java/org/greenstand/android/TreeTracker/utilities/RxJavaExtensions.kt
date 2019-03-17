package org.greenstand.android.TreeTracker.utilities

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

//Observable
fun <T> Observable<T>.applyIoScheduler() = applyScheduler(Schedulers.io())

fun <T> Observable<T>.applyComputationScheduler() = applyScheduler(Schedulers.computation())

private fun <T> Observable<T>.applyScheduler(scheduler: Scheduler) = subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())

//Single
fun <T> Single<T>.applyIoScheduler() = applyScheduler(Schedulers.io())

fun <T> Single<T>.applyComputationScheduler() = applyScheduler(Schedulers.computation())

private fun <T> Single<T>.applyScheduler(scheduler: Scheduler) = subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())

//Completable
fun Completable.applyIoScheduler() = applyScheduler(Schedulers.io())

fun Completable.applyComputationScheduler() = applyScheduler(Schedulers.computation())

private fun Completable.applyScheduler(scheduler: Scheduler) = subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())
