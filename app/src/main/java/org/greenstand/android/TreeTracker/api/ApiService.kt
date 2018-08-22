package org.greenstand.android.TreeTracker.api

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.ForgotPasswordRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.api.models.requests.RegisterRequest
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse
import org.greenstand.android.TreeTracker.api.models.responses.UserTree

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @get:GET("trees/details/user/")
    val treesForUser: Call<List<UserTree>>

    @POST("trees/create")
    fun createTree(@Body newTree: NewTreeRequest): Call<PostResult>

    @POST("auth/token")
    fun signIn(@Body authenticationRequest: AuthenticationRequest): Call<TokenResponse>

    @POST("auth/register")
    fun register(@Body registerRequest: RegisterRequest): Call<TokenResponse>

    @POST("auth/forgot")
    fun passwordReset(@Body forgotPasswordRequest: ForgotPasswordRequest): Call<Void>

    companion object {

        val ENDPOINT = BuildConfig.BASE_URL
    }

}
