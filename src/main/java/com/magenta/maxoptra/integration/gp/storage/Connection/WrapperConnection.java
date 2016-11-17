package com.magenta.maxoptra.integration.gp.storage.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.sql.Connection;

@RequestScoped
public class WrapperConnection {
    static final Logger log = LoggerFactory.getLogger(WrapperConnection.class);

    @Inject
    Connection connection = null;

    @PreDestroy
    public void preDestroy() {
        try {
            if (connection != null) {
                connection.close();
                log.info("Close connection");
            }
        } catch (Exception e) {
            log.error("Error when try close connection", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
