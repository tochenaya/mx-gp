package com.magenta.maxoptra.integration.gp.connection.http.email;

import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.EmailConf;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@Stateless
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Inject
    private ConfigurationUtils conf;

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");

    @Asynchronous
    public void sendErrors(Account account, String message) {
        EmailConf e = account.emailConf;
        String footer = "";
        sendEmail(message, account.maxoptra.accountId, e.host, e.port, e.login, e.password, e.encrypting, e.fromAddress, e.fromName,
                e.whenError.to, e.whenError.subject, e.whenError.bodyTitle, footer);
    }

    protected void sendEmail(String message, String accountName, String host, int port, String userName, String password,
                             String encrypting, String fromAddress, String fromName, String to, String subject, String bodyTitle, String footer) {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(host);
        email.setSmtpPort(port);
        if (StringUtils.isNotBlank(encrypting) && !EncryptingType.NONE.name().equals(encrypting)) {
            if (EncryptingType.Start_TLS.name().equals(encrypting)) {
                email.setStartTLSEnabled(true);
            } else if (EncryptingType.SSL_TLS.name().equals(encrypting)) {
                email.setSSLOnConnect(true);
            } else {
                log.error("Unknown encrypting type: " + encrypting, new Exception());
            }
        }
        String signature = "\n\n" + "Thank you\n" + "Your Maxoptra Team\n" + "Please do not reply to this e-mail. If you have any questions, please contact Maxoptra support team at maxoptra-support@magenta-technology.com";

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            email.setAuthentication(userName, password);
            email.setCharset(String.valueOf(StandardCharsets.UTF_8));
            if (to.contains(",")) {
                String[] tos = to.split(",");
                for (String s : tos) {
                    email.addTo(s);
                }
            } else {
                email.addTo(to);
            }

            if (StringUtils.isBlank(fromName)) fromName = fromAddress;

            email.setFrom(fromAddress, fromName);
            email.setSubject(subject + " (" + accountName + ") " + sdf.format(calendar.getTime()));

//            email.setHtmlMsg(message);
            email.setTextMsg(bodyTitle + "\n\n" + message + "\n\n" + footer + signature);

            email.send();
        } catch (EmailException ex) {
            log.error("Can't send email to: " + to + " with errors:\n" + message + "\nBecause: ", ex);
        }
    }

    public enum EncryptingType {
        SSL_TLS,
        Start_TLS, //Используется в Exchange
        NONE //Использовалось на старом почтовом сервере
    }
}
