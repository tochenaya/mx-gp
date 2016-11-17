package com.magenta.maxoptra.integration.gp.connection.http.rest;

import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class RestExceptionHandler implements ExceptionMapper<Exception> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Rest.class);

    @Override
    public Response toResponse(Exception exception) {
        log.error("Error when try unmarshall rest call parameter", exception);
        return Response.serverError().entity("").type(MediaType.APPLICATION_XML).build();
    }
}
