package com.magenta.maxoptra.integration.gp.pojo.geopalRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobData {
    String id;
    String identifier;
    String template_id;
    Integer job_status_id;
    Integer company_id; //account_id
    Long customer_id;
    //    int person_id;
    Integer address_id;
    //int asset_id;
    String prefered_start_date_time;
    String prefered_stop_date_time;
    String priority_text;
    Integer estimated_duration; //in sec
    String notes;

    String updated_on;

    Person person = new Person();
    Address address = new Address();
    Asset asset = new Asset();
    Customer customer = new Customer();
    @JsonProperty("job_fields")
    List<JobField> jobFields = new ArrayList<>();
    @JsonProperty("assigned_to")
    AssignedTo assignedTo = new AssignedTo();
    //String assigned_at; не нужно

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Integer getJob_status_id() {
        return job_status_id;
    }

    public void setJob_status_id(Integer job_status_id) {
        this.job_status_id = job_status_id;
    }

    public Integer getCompany_id() {
        return company_id;
    }

    public void setCompany_id(Integer company_id) {
        this.company_id = company_id;
    }

    public Long getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(Long customer_id) {
        this.customer_id = customer_id;
    }

    public Integer getAddress_id() {
        return address_id;
    }

    public void setAddress_id(Integer address_id) {
        this.address_id = address_id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getPriority_text() {
        return priority_text;
    }

    public void setPriority_text(String priority_text) {
        this.priority_text = priority_text;
    }

    public Integer getEstimated_duration() {
        return estimated_duration;
    }

    public void setEstimated_duration(Integer estimated_duration) {
        this.estimated_duration = estimated_duration;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPrefered_start_date_time(String prefered_start_date_time) {
        this.prefered_start_date_time = prefered_start_date_time;
    }

    public void setPrefered_stop_date_time(String prefered_stop_date_time) {
        this.prefered_stop_date_time = prefered_stop_date_time;
    }

    public String getPrefered_start_date_time() {
        return prefered_start_date_time;
    }

    public String getPrefered_stop_date_time() {
        return prefered_stop_date_time;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<JobField> getJobFields() {
        return jobFields;
    }

    public void setJobFields(List<JobField> jobFields) {
        this.jobFields = jobFields;
    }

    public AssignedTo getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(AssignedTo assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getUpdated_on() {
        return updated_on;
    }

    public void setUpdated_on(String updated_on) {
        this.updated_on = updated_on;
    }

    @Override
    public String toString() {
        StringBuilder jobFieldsBuilder = new StringBuilder();
        jobFields.stream().forEach(e -> jobFieldsBuilder.append(e.toString()));
        return "JobData{" +
                "id=" + id +
                ", identifier=" + identifier +
                ", job_status_id=" + job_status_id +
                ", company_id=" + company_id +
                ", customer_id=" + customer_id +
                ", address_id=" + address_id +
                ", prefered_start_date_time='" + prefered_start_date_time + '\'' +
                ", prefered_stop_date_time='" + prefered_stop_date_time + '\'' +
                ", priority_text='" + priority_text + '\'' +
                ", estimated_duration=" + estimated_duration +
                ", notes='" + notes + '\'' +
                ", person=" + person +
                ", address=" + address +
                ", asset=" + asset +
                ", customer=" + customer +
                ", assignedTo=" + assignedTo +
                ", template_id=" + template_id +
                ", updated_on=" + updated_on +
                ", jobFields=" + jobFieldsBuilder.toString() +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobField {
        Long template_field_id;
        String name;
        String action; // тип (text, list, listmultiple)
        String action_values; //список возможных значений
        String action_value_entered; //само значение

        public Long getTemplate_field_id() {
            return template_field_id;
        }

        public void setTemplate_field_id(Long template_field_id) {
            this.template_field_id = template_field_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAction_values() {
            return action_values;
        }

        public void setAction_values(String action_values) {
            this.action_values = action_values;
        }

        public String getAction_value_entered() {
            return action_value_entered;
        }

        public void setAction_value_entered(String action_value_entered) {
            this.action_value_entered = action_value_entered;
        }

        /**
         * Тип (text, list, listmultiple)
         */
        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return "JobField{" +
                    "template_field_id=" + template_field_id +
                    ", name='" + name + '\'' +
                    ", action='" + action + '\'' +
                    ", action_values='" + action_values + '\'' +
                    ", action_value_entered='" + action_value_entered + '\'' +
                    '}';
        }
    }
}
