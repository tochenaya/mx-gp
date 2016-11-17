package com.magenta.maxoptra.integration.gp.connection.http.rest;

import com.magenta.maxoptra.integration.gp.application.GeopalApiService;
import com.magenta.maxoptra.integration.gp.application.Mediator;
import com.magenta.maxoptra.integration.gp.application.PerformerServices;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.Configurations;
import com.magenta.maxoptra.integration.gp.connection.http.email.EmailService;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.JobData;
import com.magenta.maxoptra.integration.gp.pojo.webhook.ChangeOrderStatusRecord;
import com.magenta.maxoptra.integration.gp.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class Rest {

    private static final Logger log = LoggerFactory.getLogger(Rest.class);

    @Inject
    ConfigurationUtils configurationUtils;

    @EJB
    private Mediator mediator;

    @EJB
    EmailService emailService;

    @Inject
    private Storage storage;

    @Inject
    private PerformerServices performerServices;

    @Inject
    GeopalApiService geopalApiService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("dataExchange")
    public Response dataExchange(JobData jobData) {
        log.info("Call rest service: dataExchange(Geopal)");
        mediator.toMaxoptra(jobData);
        return Response.ok("<receive>" + jobData.toString() + "</receive>").build();
    }

    @GET
    @Path("hello")
    public Response hello() {
        try {
            geopalApiService.jobSearchIds(configurationUtils.getConfigurations().accounts.account.get(0));
        } catch (Exception e) {
            log.error("beda", e);
        }
        return Response.ok().build();
    }

    @GET
    @Path("getConfig")
    public Response getConfig() {
        log.info("Call rest service: getConfig");
        Configurations configurations;
        try {
            configurations = configurationUtils.getConfigurations();
        } catch (Exception e) {
            configurations = new Configurations();
        }
        log.info(configurations.toString());
        return Response.ok(configurations.toString()).build();
    }

    @GET
    @Path("control/clearPerformerCash")
    public Response clearPerformerCash() {
        log.info("Cell rest service: clearPerformerCash");
        performerServices.clearMap();
        return Response.ok().build();
    }

    @POST
    @Path("changeOrderStatus")
    @Consumes(MediaType.TEXT_XML)
    public Response changeOrderStatus(ChangeOrderStatusRecord changeOrderStatusRecord) {
        log.info("Call rest service: changeOrderStatus(Maxoptra)");
        mediator.toGeopal(changeOrderStatusRecord);
        return Response.ok().build();
    }

    @GET
    @Path("control/reloadConfigs")
    public Response reloadConfigs() {
        mediator.reloadConfigs();
        return Response.ok("reloadConfigs").build();
    }

}
