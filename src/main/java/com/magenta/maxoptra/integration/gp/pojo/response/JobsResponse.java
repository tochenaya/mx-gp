package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobsResponse extends Response{
    @JsonProperty("jobs")
    private List<Job> Jobs = new ArrayList<>();

    public List<Job> getJobs() {
        return Jobs;
    }

    public void setJobs(List<Job> Jobs) {
        this.Jobs = Jobs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Job {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
