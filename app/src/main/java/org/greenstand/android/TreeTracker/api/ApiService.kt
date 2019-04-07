package org.greenstand.android.TreeTracker.api

import kotlinx.coroutines.Deferred
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.api.models.requests.*
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse
import org.greenstand.android.TreeTracker.api.models.responses.UserTree

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @get:GET("trees/details/user/")
    val treesForUser: Call<List<UserTree>>

    @POST("trees/create")
    fun createTree(@Body newTree: NewTreeRequest): Deferred<PostResult>

    @POST("auth/token")
    fun signIn(@Body authenticationRequest: AuthenticationRequest): Deferred<TokenResponse>

    @PUT("devices/")
    fun updateDevice(@Body deviceRequest: DeviceRequest): Deferred<PostResult>

    @POST("planters/registration")
    fun createPlanterRegistration(@Body registration: RegistrationRequest): Deferred<PostResult>

    companion object {

        const val ENDPOINT = BuildConfig.BASE_URL
    }

}
