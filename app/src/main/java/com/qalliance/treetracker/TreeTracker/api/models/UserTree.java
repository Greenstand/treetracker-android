package com.qalliance.treetracker.TreeTracker.api.models;

import com.google.gson.annotations.SerializedName;


public class UserTree {

    @SerializedName("id")
    private String id;
    @SerializedName("created")
    private String created;
    @SerializedName("updated")
    private String updated;
    @SerializedName("priority")
    private String priority;
    @SerializedName("lat")
    private String lat;
    @SerializedName("lng")
    private String lng;
//    @SerializedName("gps")
//    private String gps;
//    @SerializedName("next_update")
//    private String nextUpdate;
    @SerializedName("imageUrl")
    private String imageUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
