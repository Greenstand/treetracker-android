package org.greenstand.android.TreeTracker.api.models.responses

import com.google.gson.annotations.SerializedName


data class UserTree(@SerializedName("id")
                    val id: String? = null,
                    @SerializedName("created")
                    val created: String? = null,
                    @SerializedName("updated")
                    val updated: String? = null,
                    @SerializedName("priority")
                    val priority: String? = null,
                    @SerializedName("lat")
                    val lat: String? = null,
                    @SerializedName("lng")
                    val lng: String? = null,
                    @SerializedName("image_url")
                    val imageUrl: String? = null)
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
