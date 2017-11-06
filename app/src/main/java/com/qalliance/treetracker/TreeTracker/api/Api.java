package com.qalliance.treetracker.TreeTracker.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class Api {

    private static Api sInstance;
    private ApiService api;
    private OkHttpClient mOkHttpClient;

    public static Api instance() {
        if (sInstance == null) {
            sInstance = new Api();
        }
        return sInstance;
    }

    public ApiService getApi() {
        if (api == null) createApi();
        return api;
    }

    private void createApi() {
        mOkHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(logInterceptor())
                .build();
        api = new Retrofit.Builder()
                .client(mOkHttpClient)
                .baseUrl(ApiService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    private static HttpLoggingInterceptor logInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Timber.tag("OkHttp").d(message);
            }
        });

        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return httpLoggingInterceptor;
    }
}
