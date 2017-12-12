package org.greenstand.android.TreeTracker.api;

import org.greenstand.android.TreeTracker.api.models.NewTree;
import org.greenstand.android.TreeTracker.api.models.PostResult;
import org.greenstand.android.TreeTracker.api.models.UserTree;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    String ENDPOINT = "http://treetracker.org/trees/";

    @GET("details/user/{id}")
    Call<List<UserTree>> getTreesForUser(@Path("id") long userId);

    @POST("create")
    Call<PostResult> createTree(@Body NewTree newTree);
}
