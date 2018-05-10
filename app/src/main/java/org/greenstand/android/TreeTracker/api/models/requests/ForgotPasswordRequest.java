package org.greenstand.android.TreeTracker.api.models.requests;

import com.google.gson.annotations.SerializedName;

/**
 * Created by zaven on 4/8/18.
 */

public class ForgotPasswordRequest {

    @SerializedName("client_id")
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
