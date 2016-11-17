package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Customer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerResponse extends Response {
    Customer customer = new Customer();

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @Override
    public String toString() {
        return "CustomerResponse{" +
                "customer=" + customer +
                '}';
    }
}
