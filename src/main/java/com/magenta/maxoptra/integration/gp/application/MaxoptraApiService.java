package com.magenta.maxoptra.integration.gp.application;

import com.magenta.maxoptra.integration.commons.orders.Order;
import com.magenta.maxoptra.integration.commons.orders.OrderStatusesResponse;
import com.magenta.maxoptra.integration.commons.orders.ResponseError;
import com.magenta.maxoptra.integration.commons.orders.requests.OrderDeleteRequest;
import com.magenta.maxoptra.integration.commons.orders.requests.OrderSaveRequest;
import com.magenta.maxoptra.integration.commons.orders.requests.OrderUpdateRequest;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.GPErrorException;
import com.magenta.maxoptra.integration.jaxb.JobDetailsRequest;
import com.magenta.maxoptra.integration.jaxb.JobDetailsResponse;
import com.magenta.maxoptra.integration.jaxb.record.JobDetailsRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class MaxoptraApiService {

    private static final Logger log = LoggerFactory.getLogger(MaxoptraApiService.class);

    @Inject
    private MxApiClientFactory mxApiClientFactory;

    public void createJob(Account account, String reference, List<OrderSaveRequest.Orders.Order> orders) throws Exception {

        com.magenta.maxoptra.integration.commons.orders.OrderResponse response = mxApiClientFactory.getMxApiClient(account).saveOrder(orders);

        StringBuilder stringBuilderError = new StringBuilder();
        StringBuilder stringBuilderSuccess = new StringBuilder();

        if (response.getError() != null) {
            stringBuilderError.append("Job ").append(reference).append(" was not imported to Maxoptra due to error ");
            stringBuilderError.append(response.getError().getErrorCode()).append(": ").append(response.getError().getErrorMessage()).append("\n");
        }

        if (response.getOrders() != null) {
            for (Order order : response.getOrders().getOrder()) {
                if (order.getErrors() != null) {
                    stringBuilderError.append("Job ").append(order.getOrderReference()).append(" was not imported to Maxoptra due to error ");

                    for (ResponseError error : order.getErrors().getError()) {
                        stringBuilderError.append(error.getErrorCode()).append(": ").append(error.getErrorMessage()).append("\n");
                    }
                } else {
                    stringBuilderSuccess.append("Job ").append(order.getOrderReference()).append(" was ").append(order.getStatus()).append("\n");
                }

            }
        }

        if (StringUtils.isNotBlank(stringBuilderError.toString())) {
            throw new GPErrorException(stringBuilderError.toString());
        } else if (StringUtils.isNotBlank(stringBuilderSuccess.toString())) {
            log.info(stringBuilderSuccess.toString());
        }
    }

    public JobDetailsRecord getMXOrderDetails(Account account, String reference) throws Exception {
        JobDetailsRequest job = new JobDetailsRequest();
        job.jobReference = reference;

        JobDetailsResponse jobDetailsResponse = mxApiClientFactory.getMxApiClient(account).getJobDetails(job);

        String code = null;
        String message = null;
        if (jobDetailsResponse.error != null) {
            code = jobDetailsResponse.error.getErrorCode();
            message = jobDetailsResponse.error.getErrorMessage();
        }
        if (jobDetailsResponse.getErrors() != null) {
            code = jobDetailsResponse.getErrors().getError().get(0).getErrorCode();
            if (code.equals("1108")) return null; //Job with the provided reference number was not found
            message = jobDetailsResponse.getErrors().getError().get(0).getErrorMessage();
        }
        if (StringUtils.isNotBlank(code) && StringUtils.isNotBlank(message)) {
            throw new GPErrorException("Error during getting job details from Maxoptra.\n Error " + code +
                    ": " + message);
        }

        JobDetailsRecord jobDetailsRecord = null;
        if (jobDetailsResponse.jobs != null) {
            jobDetailsRecord = jobDetailsResponse.jobs.get(0);
        }
        return jobDetailsRecord;
    }

    public boolean deleteMxJob(Account account, String reference) throws Exception {
        List<OrderDeleteRequest.Orders.Order> orders = new ArrayList<>();
        OrderDeleteRequest.Orders.Order order = new OrderDeleteRequest.Orders.Order();
        order.setOrderReference(reference);
        orders.add(order);
        com.magenta.maxoptra.integration.commons.orders.OrderResponse response = mxApiClientFactory.getMxApiClient(account).deleteOrder(orders);

        StringBuilder stringBuilderError = new StringBuilder();
        StringBuilder stringBuilderSuccess = new StringBuilder();

        if (response.getError() != null) {
            stringBuilderError.append("Job ").append(reference).append(" was not deleted from Maxoptra due to error ");
            stringBuilderError.append(response.getError().getErrorCode() + ": " + response.getError().getErrorMessage() + "\n");
        }

        if (response.getOrders() != null) {
            for (Order orderResponse : response.getOrders().getOrder()) {
                if (orderResponse.getErrors() != null) {
                    stringBuilderError.append("Job " + orderResponse.getOrderReference() + " was not deleted from Maxoptra due to error ");

                    for (ResponseError error : orderResponse.getErrors().getError()) {
                        stringBuilderError.append(error.getErrorCode() + ": " + error.getErrorMessage() + "\n");
                    }
                } else {
                    stringBuilderSuccess.append("Job " + orderResponse.getOrderReference() + " was " + orderResponse.getStatus() + "\n");
                }

            }
        }

        if (StringUtils.isNotBlank(stringBuilderError.toString())) {
            throw new GPErrorException(stringBuilderError.toString());
        } else if (StringUtils.isNotBlank(stringBuilderSuccess.toString())) {
            log.info(stringBuilderSuccess.toString());
        }
        return true;
    }

    public String getMxStatusByReference(Account account, String reference) throws Exception {
        String status = "";
        List<String> orders = new ArrayList<>();
        orders.add(reference);
        OrderStatusesResponse orderStatusesResponse = mxApiClientFactory.getMxApiClient(account).getOrderStatus(orders);

        if (orderStatusesResponse.getError() != null) {
            throw new GPErrorException("Error during getting job status by Maxoptra.\n Error " + orderStatusesResponse.getError().getErrorCode() + ": " + orderStatusesResponse.getError().getErrorMessage());
        }

        if (orderStatusesResponse.getOrderStatusResponse().getUnknownReferences() != null) {
            String notFoundReference = orderStatusesResponse.getOrderStatusResponse().getUnknownReferences().getReference().get(0);
            throw new GPErrorException("Job with reference " + notFoundReference + " don't found in Maxoptra");
        } else {
            OrderStatusesResponse.OrderStatusResponse.Orders.Order order = orderStatusesResponse.getOrderStatusResponse().getOrders().getOrder().get(0);
            status = order.getStatus();
        }
        return status;
    }

    public void updateJob(Account account, String reference, List<OrderUpdateRequest.Orders.Order> orders) throws Exception {

        com.magenta.maxoptra.integration.commons.orders.OrderResponse response = mxApiClientFactory.getMxApiClient(account).updateOrder(orders);

        StringBuilder stringBuilderError = new StringBuilder();
        StringBuilder stringBuilderSuccess = new StringBuilder();

        if (response.getError() != null) {
            stringBuilderError.append("Job " + reference + " was not update in Maxoptra due to error ");
            stringBuilderError.append(response.getError().getErrorCode() + ": " + response.getError().getErrorMessage() + "\n");
        }

        if (response.getOrders() != null) {
            for (Order order : response.getOrders().getOrder()) {
                if (order.getErrors() != null) {
                    stringBuilderError.append("Job " + order.getOrderReference() + " was not update in Maxoptra due to error ");

                    for (ResponseError error : order.getErrors().getError()) {
                        stringBuilderError.append(error.getErrorCode() + ": " + error.getErrorMessage() + "\n");
                    }
                } else {
                    stringBuilderSuccess.append("Job " + order.getOrderReference() + " was " + order.getStatus() + "\n");
                }

            }
        }

        if (StringUtils.isNotBlank(stringBuilderError.toString())) {
            throw new GPErrorException(stringBuilderError.toString());
        } else if (StringUtils.isNotBlank(stringBuilderSuccess.toString())) {
            log.info(stringBuilderSuccess.toString());
        }
    }
}
