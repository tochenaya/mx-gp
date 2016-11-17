package com.magenta.maxoptra.integration.gp.configuration;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

public class JobFieldsConf {

    @XmlElement(name = "jobField")
    public List<JobFieldConf> jobFieldConfs = new ArrayList<>();

    public static class JobFieldConf {
        public String mxFieldName;
        public Long gpFieldId;
    }
}
