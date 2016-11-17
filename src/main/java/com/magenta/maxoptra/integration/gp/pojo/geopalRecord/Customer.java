package com.magenta.maxoptra.integration.gp.pojo.geopalRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {
    Integer id;
    String name;
    String phone_office;
    String email;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone_office() {
        return phone_office;
    }

    public void setPhone_office(String phone_office) {
        this.phone_office = phone_office;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone_office='" + phone_office + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
