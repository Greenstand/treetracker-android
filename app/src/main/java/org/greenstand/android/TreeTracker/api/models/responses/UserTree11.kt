package org.greenstand.android.TreeTracker.api.models.responses

import com.google.gson.annotations.SerializedName


class UserTree {

    @SerializedName("id")
    var id: String? = null
    @SerializedName("created")
    var created: String? = null
    @SerializedName("updated")
    var updated: String? = null
    @SerializedName("priority")
    var priority: String? = null
    @SerializedName("lat")
    var lat: String? = null
    @SerializedName("lng")
    var lng: String? = null
    //    @SerializedName("gps")
    //    private String gps;
    //    @SerializedName("next_update")
    //    private String nextUpdate;
    //    public String getGps() {
    //        return gps;
    //    }
    //
    //    public void setGps(String gps) {
    //        this.gps = gps;
    //    }
    //
    //    public String getNextUpdate() {
    //        return nextUpdate;
    //    }
    //
    //    public void setNextUpdate(String nextUpdate) {
    //        this.nextUpdate = nextUpdate;
    //    }

    @SerializedName("image_url")
    var imageUrl: String? = null
}
