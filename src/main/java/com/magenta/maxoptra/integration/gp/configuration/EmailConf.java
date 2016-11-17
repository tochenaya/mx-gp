package com.magenta.maxoptra.integration.gp.configuration;

import com.magenta.maxoptra.integration.gp.connection.http.email.EmailService;

public class EmailConf {

    public String host;
    public int port;
    public String login;
    public String password;
    public String fromAddress;
    public String fromName;
    public String encrypting = EmailService.EncryptingType.Start_TLS.name();
    public EmailElement whenError = new EmailElement();

    public static class EmailElement {
        public String to;
        public String subject;
        public String bodyTitle;
    }

}
