package com.magenta.maxoptra.integration.gp.timer;

import com.magenta.maxoptra.integration.gp.application.Mediator;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.TimerElement;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.FailRequestException;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.GPErrorException;
import com.magenta.maxoptra.integration.gp.pojo.webhook.ChangeOrderStatusRecord;
import com.magenta.maxoptra.integration.gp.storage.Storage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//todo: в кластере это запустится на всех нодах. По идее нужно делать HA Singleton Deployments из WildFly 10. https://developer.jboss.org/wiki/WildFly1000CR4ReleaseNotes
@Singleton
@Startup
public class FailTimerService {

    protected static final int SECONDS_PER_DAY = 86400;
    private final static Logger log = LoggerFactory.getLogger(FailTimerService.class);

    @Inject
    private ConfigurationUtils conf;

    @EJB
    private Mediator mediator;

    @Inject
    private Storage storage;

    @Resource
    private javax.ejb.TimerService timerService;

    @PostConstruct
    public void initTimers() {
        try {
            for (Account account : conf.getAccountList()) {
                log.info("Init timers by account: " + account.maxoptra.accountId);
                try {
                    String key = account.maxoptra.accountId;
                    TimerElement timerElement = account.timerConf.toGeopalTimerElement;
                    ScheduleExpression expression = createSchedule(timerElement);
                    TimerConfig timerConfig = new TimerConfig(key, false);
                    timerService.createCalendarTimer(expression, timerConfig);
                } catch (Exception ex) {
                    log.error("Error when init import timer by account " + account.maxoptra.accountId + ": ", ex);
                }
            }
        } catch (Exception e) {
            log.error("Error when try to get account list: ", e);
        }
    }

    protected ScheduleExpression createSchedule(TimerElement te) {
        ScheduleExpression expression = new ScheduleExpression();
        if (StringUtils.isNotBlank(te.second)) {
            expression.second(te.second).
                    minute(te.minute == null ? "*" : te.minute).
                    hour(te.hour == null ? "*" : te.hour);
        } else if (StringUtils.isNotBlank(te.minute)) {
            expression.minute(te.minute).
                    hour(te.hour == null ? "*" : te.hour).
                    month(te.month == null ? "*" : te.month);
        } else if (StringUtils.isNotBlank(te.hour)) {
            expression.hour(te.hour).
                    month(te.month == null ? "*" : te.month).
                    year(te.year == null ? "*" : te.year);
        } else if (StringUtils.isNotBlank(te.month)) {
            expression.month(te.month).
                    year(te.year == null ? "*" : te.year);
        } else if (StringUtils.isNotBlank(te.year)) {
            expression.year(te.year);
        } else {
            expression.minute("*/5").hour("*");
        }
        return expression;
    }

    @Timeout
    public void timeout(Timer timer) {
        try {
            String event = (String) timer.getInfo();
            if (StringUtils.isNotBlank(event)) {
                Account account = conf.getAccountByMxAccountName(event);
                log.info("Execute import timer by account: " + account.maxoptra.accountId);

                List<ChangeOrderStatusRecord> changeOrderStatusRecords = storage.getStateByAccount(account);
                log.info("Storage size = " + changeOrderStatusRecords.size());

                ArrayList<ChangeOrderStatusRecord> forRemoving = new ArrayList<>();

                for (ChangeOrderStatusRecord changeOrderStatusRecord : changeOrderStatusRecords) {
                    LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
                    LocalDateTime eventDateTime = LocalDateTime.now(ZoneId.of("UTC"));
                    if(changeOrderStatusRecord.eventDateTime != null) {
                        eventDateTime = LocalDateTime.parse(changeOrderStatusRecord.eventDateTime,
                                DateTimeFormatter.ofPattern(account.maxoptra.dateTimePattern));
                    }

                    if (Duration.between(now, eventDateTime).getSeconds() > SECONDS_PER_DAY) {
                        forRemoving.add(changeOrderStatusRecord);
                    } else {

                        try {
                            mediator.toGeopal(changeOrderStatusRecord, account);
                        } catch (FailRequestException e) {
                            if (!forRemoving.isEmpty()) {
                                storage.removeSelectedRecords(forRemoving);
                            }
                            return;
                        } catch (GPErrorException e){
                            log.warn("Error during send to Geopal", e.getMessage());
                            mediator.sendResults(account, e.getMessage());
                            forRemoving.add(changeOrderStatusRecord);
                        }

                        forRemoving.add(changeOrderStatusRecord);
                    }
                }

                if (!forRemoving.isEmpty()) {
                    storage.removeSelectedRecords(forRemoving);
                }
            }
        } catch (Exception ex) {
            log.error("Error when invoke timer: ", ex);
        }
    }

    public void reInitTimers() {
        log.info("Call re init timers");
        stopAllTimers();
        initTimers();
    }

    protected void stopAllTimers() {
        log.info("Stop all timers");
        for (Timer timer : timerService.getAllTimers()) {
            timer.cancel();
        }
    }

}
