package com.magenta.maxoptra.integration.gp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magenta.maxoptra.integration.commons.orders.OrderStatusUpdate;
import com.magenta.maxoptra.integration.commons.orders.OrderStatusUpdateResponse;
import com.magenta.maxoptra.integration.commons.orders.Performer;
import com.magenta.maxoptra.integration.commons.orders.ResponseError;
import com.magenta.maxoptra.integration.commons.orders.requests.OrderSaveRequest;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.Statuses;
import com.magenta.maxoptra.integration.gp.connection.http.HttpService;
import com.magenta.maxoptra.integration.gp.connection.http.email.EmailService;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.FailRequestException;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.GPErrorException;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Asset;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.JobData;
import com.magenta.maxoptra.integration.gp.pojo.response.AssetResponse;
import com.magenta.maxoptra.integration.gp.pojo.response.JobDataResponse;
import com.magenta.maxoptra.integration.gp.pojo.response.JobsResponse;
import com.magenta.maxoptra.integration.gp.pojo.response.Response;
import com.magenta.maxoptra.integration.gp.pojo.webhook.ChangeOrderStatusRecord;
import com.magenta.maxoptra.integration.gp.storage.Storage;
import com.magenta.maxoptra.integration.gp.timer.FailTimerService;
import com.magenta.maxoptra.integration.gp.xml.XMLService;
import com.magenta.maxoptra.integration.jaxb.record.JobDetailsRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;

@Stateless
public class Mediator {

    private static final Logger log = LoggerFactory.getLogger(Mediator.class);

    @Inject
    private HttpService httpService;

    @Inject
    private GeopalApiService geopalApiService;

    @Inject
    private MaxoptraApiService maxoptraApiService;

    @Inject
    private Mapper mapper;

    @Inject
    private ConfigurationUtils configurationUtils;

    @Inject
    private MxApiClientFactory mxApiClientFactory;

    @Inject
    private PerformerServices performerServices;

    @EJB
    EmailService emailService;

    @Inject
    Storage storage;

    @Inject
    FailTimerService failTimerService;

    @Inject
    ArchiveMessageComponent archiveMessageComponent;

    @Asynchronous
    public void toMaxoptra(JobData jobData) {
        Account account = null;
        String reference = jobData.getIdentifier() + "/" + jobData.getId();
        try {
            log.info("jobData from geopal: " + jobData);
            account = configurationUtils.getAccountByGpTemplateId(jobData.getTemplate_id());
            if (account == null) {
                log.warn("Account not found in configuration by template Id = " + jobData.getTemplate_id());
                return;
            }
            archiveMessageComponent.add("Send by Iot Project from GeoPal", new ObjectMapper().writeValueAsString(jobData));
            boolean ok = false;

            if (jobData.getJob_status_id() == GeopalStatuses.UNASSIGNED.ordinal()) {
                JobDataResponse jobDataResponse = geopalApiService.getJobDetailsById(jobData.getId(), account.geopal);
                ok = createMxJob(account, jobDataResponse.getJob());

            } else if (jobData.getJob_status_id() == GeopalStatuses.DELETED.ordinal()) {
                ok = maxoptraApiService.deleteMxJob(account, reference);

            } else {
                String currentMxStatus = maxoptraApiService.getMxStatusByReference(account, reference);
                ok = changeMxJobStatus(account, jobData, currentMxStatus);
            }

            if (ok) {
                ArchiveService.archiveImportMessage(account.archiveConf, archiveMessageComponent.getMessage());
            }

        } catch (Exception e) {
            if(e instanceof GPErrorException){
                log.warn("Error during send to Maxoptra. ", e);
            } else {
                log.error("Error during send to Maxoptra. ", e);
            }
            ArchiveService.archiveErrorMessage(account.archiveConf, archiveMessageComponent.getMessage(), e);

            if (account != null) {
                sendResults(account, "An error occurred while sending job " + reference + " to Maxoptra. " + e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }
    }

    @Asynchronous
    public void toGeopal(ChangeOrderStatusRecord changeOrderStatusRecord) {
        Account account = null;
        try {
            try {

                archiveMessageComponent.add("Webhook from Maxoptra", XMLService.marshal(changeOrderStatusRecord));

                log.info("changeOrderStatusRecord: " + changeOrderStatusRecord);
                if (!isAssign(changeOrderStatusRecord) && !isUnAssign(changeOrderStatusRecord)) {
                    return;
                }

                account = configurationUtils.getAccountByMxAccountName(changeOrderStatusRecord.account);
                if (account == null) {
                    log.warn("Account not found in configuration by Maxoptra account = " + changeOrderStatusRecord.account);
                    return;
                }


                if (!storage.getState().isEmpty()) {
                    storage.add(changeOrderStatusRecord);
                    return;
                }

                boolean ok = toGeopal(changeOrderStatusRecord, account);
                if (ok) {
                    ArchiveService.archiveExportMessage(account.archiveConf, archiveMessageComponent.getMessage());
                }

            } catch (FailRequestException e) {
                storage.add(changeOrderStatusRecord);
                throw new GPErrorException("GeoPal is unavailable. Request added to the storage. When GeoPal is available " +
                        "it will be sent again. " + e.getMessage());
            }
        } catch (Exception e) {
            if(e instanceof GPErrorException){
                log.warn("Error during send to Geopal", e);
            } else {
                log.error("Error during send to Geopal", e);
            }

            ArchiveService.archiveErrorMessage(account.archiveConf, archiveMessageComponent.getMessage(), e);
            if (account != null) {
                sendResults(account, "An error occurred while sending job " + changeOrderStatusRecord.reference + " to GeoPal. " + e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }
    }

    public boolean toGeopal(ChangeOrderStatusRecord changeOrderStatusRecord, Account account) throws Exception {

        //Планирование
        if (isAssign(changeOrderStatusRecord)) {
            return assignInGeopal(account, changeOrderStatusRecord);
        }

        //Отпланирование
        if (isUnAssign(changeOrderStatusRecord)) {
            return unAssignInGeopal(account, changeOrderStatusRecord);
        }
        return false;
    }

    private boolean isAssign(ChangeOrderStatusRecord changeOrderStatusRecord) {
        return changeOrderStatusRecord.newStatus.equalsIgnoreCase("DETAILS_SENT");
    }

    private boolean isUnAssign(ChangeOrderStatusRecord changeOrderStatusRecord) {
        return (changeOrderStatusRecord.newStatus.equalsIgnoreCase("NEW") &&
                !changeOrderStatusRecord.oldStatus.equalsIgnoreCase("ALLOCATED") &&
                !changeOrderStatusRecord.oldStatus.equalsIgnoreCase("COMMITTING") &&
                !changeOrderStatusRecord.oldStatus.equalsIgnoreCase("DETAILS_SENDING"))
                //Нажатие на Unlock schedule
                || (changeOrderStatusRecord.newStatus.equalsIgnoreCase("ALLOCATED") &&
                changeOrderStatusRecord.oldStatus.equalsIgnoreCase("DETAILS_SENT"));
    }

    private boolean assignInGeopal(Account account, ChangeOrderStatusRecord changeOrderStatusRecord) throws Exception {

        String geopalId = getGeopalId(changeOrderStatusRecord.reference);
        String jobStartTime = mapper.dateFromMxToGp(changeOrderStatusRecord.jobStartTime, account);
        String employeeId = performerServices.getGPEmployeeByName(account, changeOrderStatusRecord.engineer).getId().toString();

        Response response = geopalApiService.assign(account, geopalId, jobStartTime, employeeId);

        if (response.getErrorCode() != null) {
            throw new GPErrorException("Job " + changeOrderStatusRecord.reference + " wasn't assigned in GeoPal due to error " +
                    response.getErrorCode() + ": " + response.getErrorMessage());
        }
        return true;
    }

    private boolean unAssignInGeopal(Account account, ChangeOrderStatusRecord changeOrderStatusRecord) throws Exception {

        String geopalId = getGeopalId(changeOrderStatusRecord.reference);

        Response response = geopalApiService.unAssign(account, geopalId);

        if (response.getErrorCode() != null) {
            throw new GPErrorException("Job " + changeOrderStatusRecord.reference + " wasn't unassigned in GeoPal due to error " +
                    response.getErrorCode() + ": " + response.getErrorMessage());
        }
        return true;
    }

    private String getGeopalId(String reference) throws Exception {
        try {
            return reference.split("/")[1];
        } catch (Exception e) {
            throw new GPErrorException("Job " + reference + " wasn't sent to GeoPal. Reference is not valid. It should be 'identifier/id'");
        }
    }

    private boolean changeMxJobStatus(Account account, JobData jobData, String currentMxStatus) throws Exception {
        String reference = jobData.getIdentifier() + "/" + jobData.getId();

        List<Statuses.Status> statusesConfList = account.statuses.status;
        GeopalStatuses gpStatus = GeopalStatuses.values()[jobData.getJob_status_id()];

        Optional<Statuses.Status> statusConfOptional = statusesConfList.stream().filter(e -> e.statusGp.equals(gpStatus.getDescription())).findFirst();
        if (!statusConfOptional.isPresent()) return false;
        Statuses.Status statusConf = statusConfOptional.get();

        List<OrderStatusUpdate> orderStatusUpdates = new ArrayList<>();
        OrderStatusUpdate orderStatusUpdate = new OrderStatusUpdate();

        orderStatusUpdate.setOrderReference(reference);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ConfigurationUtils.geopalDateTimePattern);
        Date changeDate = simpleDateFormat.parse(jobData.getUpdated_on());
        orderStatusUpdate.setChangeDate(changeDate);

        Performer performer = new Performer();
        String externalId;
        try {
            externalId = performerServices.getMXPerformerByName(account, jobData.getAssignedTo().getFirst_name() + " " + jobData.getAssignedTo().getLast_name())
                    .getExternalId();
        } catch (GPErrorException e) {
            throw new GPErrorException("Job " + reference + " status in Maxoptra could not be updated.\n" + e.getMessage());
        }
        performer.setExternalId(externalId);
        orderStatusUpdate.setPerformer(performer);
        orderStatusUpdates.add(orderStatusUpdate);

        int indexCurrentMxStatus = -1;
        for (int i = 0; i < statusConf.mxStatuses.statusMx.size(); i++) {
            if (statusConf.mxStatuses.statusMx.get(i).equalsIgnoreCase(currentMxStatus)) {
                indexCurrentMxStatus = i;
            }
        }
        log.info("Current status maxoptra job = " + currentMxStatus);

        for (int i = 0; i < statusConf.mxStatuses.statusMx.size(); i++) {
            String mxStatuses = statusConf.mxStatuses.statusMx.get(i);
            if (indexCurrentMxStatus >= i)
                continue; //выполняем смену статусов для всех после найденного или все если не нашли

            orderStatusUpdate.setStatus(mxStatuses.toUpperCase());

            OrderStatusUpdateResponse response = mxApiClientFactory.getMxApiClient(account).changeOrderStatus(orderStatusUpdates);

            StringBuilder stringBuilderError = new StringBuilder();

            if (response.getError() != null) {
                stringBuilderError.append("Job " + reference + " status in Maxoptra could not be updated to " + mxStatuses + " due to error ");
                stringBuilderError.append(response.getError().getErrorCode() + ": " + response.getError().getErrorMessage() + "\n");
            }

            if (response.getOrders() != null) {
                for (OrderStatusUpdate order : response.getOrders().getOrder()) {
                    if (order.getErrors() != null) {
                        stringBuilderError.append("Job " + order.getOrderReference() + " status in Maxoptra could not be updated to " +
                                order.getStatus() + " due to error ");
                        for (ResponseError error : order.getErrors().getError()) {
                            stringBuilderError.append(error.getErrorCode() + ": " + error.getErrorMessage() + "\n");
                        }
                    }
                }
            }

            if (StringUtils.isNotBlank(stringBuilderError.toString())) {
                throw new GPErrorException("Job " + reference + " status in GeoPal has changed to " + gpStatus.getDescription() + ".\n" + stringBuilderError.toString());
            } else {
                log.info("Job status in GeoPal has changed to " + gpStatus.getDescription() +
                        ".\nResponse from Maxoptra. Job " + reference + " was changed status to " + mxStatuses.toUpperCase());
            }
        }
        return true;
    }

    private boolean createMxJob(Account account, JobData jobData) throws Exception {

        String reference = jobData.getIdentifier() + "/" + jobData.getId();

        //Location берется из Asset или Address, если поле Asset не заполнено
        AssetResponse assetResponse = geopalApiService.getAssetById(jobData.getAsset().getId(), account.geopal);
        Asset asset = assetResponse != null ? assetResponse.getAsset() : null;

        List<OrderSaveRequest.Orders.Order> orders = mapper.gpToMxOrder(account, jobData, asset);

        maxoptraApiService.createJob(account, reference, orders);

        return true;
    }

    public void sendResults(Account account, String errorMessage) {
        String host;
        if (StringUtils.isBlank(errorMessage)) {
            log.info("There are without error message, nothing send to email");
            return;
        }

        host = account.emailConf.host;
        if (StringUtils.isBlank(host)) {
            log.warn("Email host name not set, email can't be send");
            return;
        }

        log.info("Email with errors will be send");
        emailService.sendErrors(account, errorMessage);
    }

    public void reloadConfigs() {
        try {
            configurationUtils.readConfigurations();
            failTimerService.reInitTimers();
        } catch (Exception e) {
            log.error("Error when reload configurations: ", e);
        }
    }

    @Asynchronous
    public void toMaxoptraMissingJobs(Account account) {
        try {
            JobsResponse jobsResponse = geopalApiService.jobSearchIds(account);

            for (JobsResponse.Job job : jobsResponse.getJobs()) {
                toMaxoptraMissingJob(account, job);
            }
            ArchiveService.archiveImportMessage(account.archiveConf, archiveMessageComponent.getMessage());
        } catch (Exception e) {
            if(e instanceof GPErrorException){
                log.warn("Error! ", e);
            } else {
                log.error("Error! ", e);
            }
            ArchiveService.archiveErrorMessage(account.archiveConf, archiveMessageComponent.getMessage(), e);
            sendResults(account, "Error during send to Maxoptra. " + e.getMessage());
        }

    }

    public void toMaxoptraMissingJob(Account account, JobsResponse.Job job) {
        String reference = job.getId();
        try {
            JobDataResponse jobDataResponse = geopalApiService.getJobDetailsById(job.getId(), account.geopal);

            JobData gpJobData = jobDataResponse.getJob();
            reference = gpJobData.getIdentifier() + "/" + gpJobData.getId();

            JobDetailsRecord mxJobDetails = maxoptraApiService.getMXOrderDetails(account, reference);

            if (gpJobData.getJob_status_id().equals(GeopalStatuses.DELETED.ordinal()) && mxJobDetails != null) {
                maxoptraApiService.deleteMxJob(account, reference);
                return;
            }

            if (gpJobData.getJob_status_id().equals(GeopalStatuses.DELETED.ordinal())) {
                return;
            }

            if (mxJobDetails == null || mxJobDetails.status.equalsIgnoreCase("NEW") || mxJobDetails.status.equalsIgnoreCase("ALLOCATED")) {
                //Создание и изменение
                createMxJob(account, gpJobData);
                return;
            }

            if (mxJobDetails != null && !mxJobDetails.status.equalsIgnoreCase("NEW") &&
                    !mxJobDetails.status.equalsIgnoreCase("ALLOCATED") &&
                    !mxJobDetails.status.equalsIgnoreCase("COMMITTING") &&
                    !mxJobDetails.status.equalsIgnoreCase("DETAILS_SENDING")) {
                changeMxJobStatus(account, gpJobData, mxJobDetails.status);
            }
        } catch (Exception e) {
            if(e instanceof GPErrorException){
                log.warn("Error! ", e);
            } else {
                log.error("Error! ", e);
            }
            sendResults(account, "Error during send to Maxoptra with job " + reference + ". " + e.getMessage());
        }
    }
}
