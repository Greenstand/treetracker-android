package org.greenstand.android.TreeTracker.api;

import org.greenstand.android.TreeTracker.BuildConfig;
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest;
import org.greenstand.android.TreeTracker.api.models.requests.ForgotPasswordRequest;
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest;
import org.greenstand.android.TreeTracker.api.models.responses.PostResult;
import org.greenstand.android.TreeTracker.api.models.requests.RegisterRequest;
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse;
import org.greenstand.android.TreeTracker.api.models.responses.UserTree;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    String ENDPOINT = BuildConfig.BASE_URL;

    @GET("trees/details/user/")
    Call<List<UserTree>> getTreesForUser();

    @POST("trees/create")
    Call<PostResult> createTree(@Body NewTreeRequest newTree);

    @POST("auth/token")
    Call<TokenResponse> signIn(@Body AuthenticationRequest authenticationRequest);

    @POST("auth/register")
    Call<TokenResponse> register(@Body RegisterRequest registerRequest);

    @POST("auth/forgot")
    Call<Void> passwordReset(@Body ForgotPasswordRequest forgotPasswordRequest);

}
