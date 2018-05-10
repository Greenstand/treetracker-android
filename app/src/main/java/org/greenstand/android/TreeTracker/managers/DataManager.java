package org.greenstand.android.TreeTracker.managers;

import com.crashlytics.android.Crashlytics;

import org.greenstand.android.TreeTracker.api.Api;
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest;
import org.greenstand.android.TreeTracker.api.models.responses.UserTree;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This is entry point for latest API, "trees/details/user" and "trees/create"
 */

public abstract class DataManager<T> {

    private static final String TAG = "DataManager";

    private Api mApi;

    public DataManager() {
        mApi = Api.instance();
    }

    public abstract void onDataLoaded(T data);

    public abstract void onRequestFailed(String message);

    public void loadUserTrees() {
        Call<List<UserTree>> trees = mApi.getApi().getTreesForUser();
        trees.enqueue(new Callback<List<UserTree>>() {
            @Override
            public void onResponse(Call<List<UserTree>> call, Response<List<UserTree>> response) {
                if (response.isSuccessful()) {
                    onDataLoaded((T) response.body());
                }
            }

            @Override
            public void onFailure(Call<List<UserTree>> call, Throwable t) {
                onRequestFailed(t.getMessage());
                Timber.tag(TAG).e(t.getMessage());
            }
        });
    }

}
