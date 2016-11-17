package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Asset;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetResponse extends Response {

    Asset asset = new Asset();

    @Override
    public String toString() {
        return super.toString() + " AssetResponse{" +
                "asset=" + asset +
                '}';
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }
}
