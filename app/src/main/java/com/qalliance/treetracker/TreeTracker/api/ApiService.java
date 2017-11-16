package com.qalliance.treetracker.TreeTracker.api;

import com.qalliance.treetracker.TreeTracker.api.models.NewTree;
import com.qalliance.treetracker.TreeTracker.api.models.PostResult;
import com.qalliance.treetracker.TreeTracker.api.models.UserTree;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    String ENDPOINT = "http://dev.treetracker.org/trees/";

    @GET("details/user/{id}")
    Call<List<UserTree>> getTreesForUser(@Path("id") long userId);

    @POST("create")
    Call<PostResult> createTree(@Body NewTree newTree);
}
