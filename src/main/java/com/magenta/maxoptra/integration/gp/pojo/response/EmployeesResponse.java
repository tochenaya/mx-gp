package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Employee;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeesResponse extends Response {

    private List<Employee> employees = new ArrayList<>();

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    @Override
    public String toString() {
        return super.toString() + "{ employees size " +
                +employees.size() +
                '}';
    }
}
