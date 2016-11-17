package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Customer;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomersResponse {
    List<Customer> customers = new ArrayList<Customer>();

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }
}
