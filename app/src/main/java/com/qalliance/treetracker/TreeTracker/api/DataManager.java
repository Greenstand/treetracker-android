package com.qalliance.treetracker.TreeTracker.api;

import com.qalliance.treetracker.TreeTracker.api.models.NewTree;
import com.qalliance.treetracker.TreeTracker.api.models.UserTree;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This is entry point for latest API, "trees/details/user" and "trees/create"
 */

public abstract class DataManager {

    private static final String TAG = "DataManager";

    private Api mApi;

    public DataManager() {
        mApi = Api.instance();
    }

    public abstract void onDataLoaded(List<UserTree> data);

    public abstract void onRequestFailed(String message);

    public void loadUserTrees(int userId) {
        Call<List<UserTree>> trees = mApi.getApi().getTreesForUser(userId);
        trees.enqueue(new Callback<List<UserTree>>() {
            @Override
            public void onResponse(Call<List<UserTree>> call, Response<List<UserTree>> response) {
                if (response.isSuccessful()) {
                    onDataLoaded(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<UserTree>> call, Throwable t) {
                onRequestFailed(t.getMessage());
                Timber.tag(TAG).e(t.getMessage());
            }
        });
    }

    public void createNewTree(NewTree newTree) {
        Call<NewTree> tree = mApi.getApi().createTree(newTree);
        tree.enqueue(new Callback<NewTree>() {
            @Override
            public void onResponse(Call<NewTree> call, Response<NewTree> response) {
                if (response.isSuccessful()) {
                    Timber.tag(TAG).d("post submitted to API.%s", response.body().toString());
                }
            }

            @Override
            public void onFailure(Call<NewTree> call, Throwable t) {
                onRequestFailed(t.getMessage());
                Timber.tag(TAG).e("Unable to submit post to API.");
            }
        });
    }

}
