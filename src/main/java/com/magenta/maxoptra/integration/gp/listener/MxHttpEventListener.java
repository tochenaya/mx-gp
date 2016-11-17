package com.magenta.maxoptra.integration.gp.listener;

import com.magenta.maxoptra.integration.event.RequestEvent;
import com.magenta.maxoptra.integration.event.ResponseEvent;
import com.magenta.maxoptra.integration.gp.application.ArchiveMessageComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

public class MxHttpEventListener {

    private static final Logger log = LoggerFactory.getLogger(MxHttpEventListener.class);

    @Inject
    ArchiveMessageComponent archiveMessageComponent;

    public void runRequestEvent(@Observes RequestEvent runRequestEvent) {
        archiveMessageComponent.add("Request", runRequestEvent.getToUrl(), runRequestEvent.getRequest());
    }

    public void runResponseEvent(@Observes ResponseEvent runResponseEvent) {
        archiveMessageComponent.add("Response", runResponseEvent.getFromUrl(), runResponseEvent.getResponse());
    }
}

