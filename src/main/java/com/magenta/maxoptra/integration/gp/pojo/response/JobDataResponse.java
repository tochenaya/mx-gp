package com.magenta.maxoptra.integration.gp.pojo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.JobData;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDataResponse extends Response{
    JobData job = new JobData();

    public JobData getJob() {
        return job;
    }

    public void setJob(JobData job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return super.toString() + " " + "JobDataResponse{" +
                "job=" + job +
                '}';
    }
}
