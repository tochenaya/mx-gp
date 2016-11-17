package com.magenta.maxoptra.integration.gp.pojo.geopalRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Asset {
    String id;
    String name;
    Integer customer_id;
    Address address = new Address();
    Customer customer = new Customer();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(Integer customer_id) {
        this.customer_id = customer_id;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", customer_id=" + customer_id +
                ", address=" + address +
                ", customer=" + customer +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetCategories {
        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "AssetCategories{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
