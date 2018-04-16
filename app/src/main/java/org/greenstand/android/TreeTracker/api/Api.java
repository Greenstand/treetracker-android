package org.greenstand.android.TreeTracker.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class Api {

    private static Api sInstance;
    private ApiService apiService;
    private OkHttpClient mOkHttpClient;
    private String authToken;

    public static Api instance() {
        if (sInstance == null) {
            sInstance = new Api();
        }
        return sInstance;
    }

    public ApiService getApi() {
        if (apiService == null) {
            createApi();
        }
        return apiService;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public boolean isLoggedIn() {
        return authToken != null;
    }

    private void createApi() {
        mOkHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(loggingInterceptor())
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        apiService = new Retrofit.Builder()
                .client(mOkHttpClient)
                .baseUrl(ApiService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    private static HttpLoggingInterceptor loggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Timber.tag("OkHttp").d(message);
                    }
                });

        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return httpLoggingInterceptor;
    }


    public class AuthenticationInterceptor implements Interceptor {

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {

            Request original = chain.request();

            if (authToken != null) {
                Request.Builder builder = original.newBuilder()
                        .header("Authorization", "Bearer " + authToken);
                Request request = builder.build();
                return chain.proceed(request);
            } else {
                return chain.proceed(original);
            }

        }

    }
}