package com.magenta.maxoptra.integration.gp.application;

import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.List;

@ApplicationScoped
public class Initialize {

    @Inject
    ConfigurationUtils configurationUtils;

    @Inject
    Mediator mediator;

    private final static Logger log = LoggerFactory.getLogger(Initialize.class);

    public void init(@Observes @Initialized(javax.enterprise.context.ApplicationScoped.class) Object init) throws FileNotFoundException, JAXBException {
        try {
            log.info("Start init mx-gp");
            List<Account> accounts = configurationUtils.getConfigurations().accounts.account;
            for (Account account: accounts){
                mediator.toMaxoptraMissingJobs(account);
            }
            log.info("Mx-gp successful started");
        } catch (Exception ex) {
            log.error("Error when starting mx-gp:", ex);
            throw new RuntimeException("Error when starting mx-gp", ex);
        }
    }

   /* @PreDestroy
    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        try {

        } catch (Exception e) {
            log.error("Error when try to shutdown storage", e);
        }
    }*/
}
