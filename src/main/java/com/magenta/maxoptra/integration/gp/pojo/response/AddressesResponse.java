package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Address;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressesResponse extends Response {
    List<Address> addresses = new ArrayList<>();

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }
}
