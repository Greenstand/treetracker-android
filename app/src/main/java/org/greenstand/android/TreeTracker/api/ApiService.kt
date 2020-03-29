package org.greenstand.android.TreeTracker.api

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.requests.PlanterAccountRequest
import org.greenstand.android.TreeTracker.api.models.responses.PlanterAccountData
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse
import org.greenstand.android.TreeTracker.api.models.responses.UserTree
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @get:GET("trees/details/user/")
    val treesForUser: Call<List<UserTree>>

    @POST("trees/create")
    suspend fun createTree(@Body newTree: NewTreeRequest): PostResult

    @POST("auth/token")
    suspend fun signIn(@Body authenticationRequest: AuthenticationRequest): TokenResponse

    @PUT("devices/")
    suspend fun updateDevice(@Body deviceRequest: DeviceRequest): PostResult

    @POST("planter_accounts")
    suspend fun planterAccountData(
        @Body planterAccountRequest: PlanterAccountRequest
    ): Set<PlanterAccountData>

    companion object {

        const val ENDPOINT = BuildConfig.BASE_URL
        const val PAYMENT_ENDPOINT = BuildConfig.PLANTER_ACCOUNT_URL
    }

}
