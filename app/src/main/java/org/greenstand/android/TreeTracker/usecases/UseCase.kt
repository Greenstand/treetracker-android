package org.greenstand.android.TreeTracker.usecases

abstract class UseCase<in Params : Any, out Result> {

    abstract suspend fun execute(params: Params): Result
}
