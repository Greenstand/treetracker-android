package org.greenstand.android.TreeTracker.api

import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest

class RetrofitApi(private val api: ApiService) {

    suspend fun createTree(newTreeRequest: NewTreeRequest): Int {
        return api.createTree(newTreeRequest).status
    }
}