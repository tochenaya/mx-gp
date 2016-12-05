package com.magenta.maxoptra.integration.gp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.GeoPalConf;
import com.magenta.maxoptra.integration.gp.connection.http.HttpService;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.GPErrorException;
import com.magenta.maxoptra.integration.gp.pojo.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class GeopalApiService {

    private static final Logger log = LoggerFactory.getLogger(GeopalApiService.class);

    @Inject
    private HttpService httpService;

    public CustomerResponse getCustomerById(String id, GeoPalConf geopal) throws Exception {
        if (id == null) return null;
        Map<String, String> params = new HashMap<>();
        params.put("customer_id", id);

        String response = httpService.get("api/customer/get", params, geopal);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response, CustomerResponse.class);
    }

    public JobDataResponse getJobDetailsById(String id, GeoPalConf geopal) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("job_id", id);

        String response = httpService.get("api/job/get", params, geopal);

        ObjectMapper objectMapper = new ObjectMapper();
        JobDataResponse jobDataResponse = objectMapper.readValue(response, JobDataResponse.class);
        log.info("jobDataResponse: " + response);

        if (jobDataResponse.getErrorCode() != null) {
            throw new GPErrorException("Error during get job detail from Geopal " +
                    jobDataResponse.getErrorCode() + ": " + jobDataResponse.getErrorMessage());
        }
        return jobDataResponse;
    }

    public AssetResponse getAssetById(String id, GeoPalConf geopal) throws Exception {
        if (id == null) return null;
        Map<String, String> params = new HashMap<String, String>();
        params.put("asset_id", id);

        String response = httpService.get("api/assets/get", params, geopal);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response, AssetResponse.class);
    }

    public Response assign(Account account, String geopalId, String jobStartTime, String employeeId) throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        params.put("job_id", geopalId);
        params.put("start_date_time", jobStartTime);
        params.put("employee_id", employeeId);

        String responseStr = httpService.post("api/job/assign", params, account.geopal);
        log.info(geopalId + " assign: " + responseStr);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(responseStr, Response.class);
    }

    public Response unAssign(Account account, String geopalId) throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        params.put("job_id", geopalId);

        String responseStr = httpService.post("api/jobs/unassign", params, account.geopal);
        log.info("UN assign: " + responseStr);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(responseStr, Response.class);
    }

    public JobsResponse jobSearchIds(Account account) throws Exception{
        if(!ZoneId.getAvailableZoneIds().contains(account.geopal.timeZone)){
            throw new GPErrorException("Zone " + account.geopal.timeZone + " in configuration file doesn't exist.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of(account.geopal.timeZone));
        String dateTimeFrom = now.minusDays(account.amountDays)
                .format(DateTimeFormatter.ofPattern(ConfigurationUtils.geopalDateTimePattern));
        String dateTimeTo = now.format(DateTimeFormatter.ofPattern(ConfigurationUtils.geopalDateTimePattern));

        Map<String, String> params = new HashMap<String, String>();
        params.put("date_time_from", dateTimeFrom);
        log.info("date_time_from = " + dateTimeFrom);
        params.put("date_time_to", dateTimeTo);
        log.info("date_time_to = " + dateTimeTo);
        params.put("job_template_id", account.geopal.jobTemplateId);

        String responseStr = httpService.get("api/jobsearch/ids", params, account.geopal);
        log.info("api/jobsearch/ids: \n" + responseStr);
        JobsResponse response = new ObjectMapper().readValue(responseStr, JobsResponse.class);

        if (response.getErrorCode() != null) {
            throw new GPErrorException("Error " +
                    response.getErrorCode() + ": " + response.getErrorMessage());
        }
        return response;
    }



}
