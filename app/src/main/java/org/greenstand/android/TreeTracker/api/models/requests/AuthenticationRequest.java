package org.greenstand.android.TreeTracker.api.models.requests;

import com.google.gson.annotations.SerializedName;

/**
 * Created by zaven on 4/8/18.
 */

public class AuthenticationRequest {

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("client_secret")
    private String clientSecret;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
