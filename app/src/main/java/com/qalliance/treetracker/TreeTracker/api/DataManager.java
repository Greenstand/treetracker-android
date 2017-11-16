package com.qalliance.treetracker.TreeTracker.api;

import com.qalliance.treetracker.TreeTracker.api.models.NewTree;
import com.qalliance.treetracker.TreeTracker.api.models.UserTree;

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

    public void loadUserTrees(long userId) {
        Call<List<UserTree>> trees = mApi.getApi().getTreesForUser(userId);
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

    public T createNewTree(NewTree newTree) {
        T result = null;
        try {
            Response<T> tree = (Response<T>) mApi.getApi().createTree(newTree).execute();
            if (tree.isSuccessful()){
                result = tree.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
