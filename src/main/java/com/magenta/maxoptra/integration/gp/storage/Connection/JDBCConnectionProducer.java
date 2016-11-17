package com.magenta.maxoptra.integration.gp.storage.Connection;

import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.DataBaseConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

@ApplicationScoped
public class JDBCConnectionProducer {

    private static final Logger log = LoggerFactory.getLogger(JDBCConnectionProducer.class);

    @Inject
    private ConfigurationUtils configurationUtils;

    private DataSource dataSource;

    @Produces
    @Default
    private Connection createConnection() {
        try {
            if (dataSource == null) {
                initDataSource();
            }
            log.info("Get Connection");
            return dataSource.getConnection();
        } catch (Exception e) {
            log.error("Can't init dataSource", e);
        }
        return null;
    }

    private void initDataSource() throws Exception {
        log.info("Init Data Source");
        InitialContext ic = new InitialContext();
        DataBaseConf dataBaseConf = configurationUtils.getConfigurations().dataBaseConf;
        dataSource = (DataSource) ic.lookup(dataBaseConf.dataSource);
    }

}
