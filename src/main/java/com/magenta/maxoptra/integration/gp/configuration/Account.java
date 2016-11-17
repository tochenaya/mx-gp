package com.magenta.maxoptra.integration.gp.configuration;

import javax.xml.bind.annotation.XmlElement;

public class Account {
    @XmlElement(name = "geopal")
    public GeoPalConf geopal = new GeoPalConf();

    @XmlElement(name = "maxoptra")
    public MaxoptraConf maxoptra = new MaxoptraConf();

    @XmlElement(name = "jobFields")
    public JobFieldsConf jobFieldsConf = new JobFieldsConf();

    public Statuses statuses = new Statuses();

    @XmlElement(name = "email")
    public EmailConf emailConf = new EmailConf();

    @XmlElement(name = "timer")
    public TimerConf timerConf = new TimerConf();

    @XmlElement(name = "archive")
    public ArchiveConf archiveConf = new ArchiveConf();

    public Long amount_days;
}
