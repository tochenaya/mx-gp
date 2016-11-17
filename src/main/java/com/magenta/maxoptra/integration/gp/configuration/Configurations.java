package com.magenta.maxoptra.integration.gp.configuration;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "configurations")
public class Configurations {

    public int httpTimeout = 30;

    public String geopalBaseUrl = "https://app.geopalsolutions.com/";

    public String storageTableName = "mxgeopalJob";

    @XmlElement(name = "database")
    public DataBaseConf dataBaseConf;

    public Accounts accounts;

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        for(Account account : accounts.account){
            result.append("/n Account (geopal (jobTemplateId = ").append(account.geopal.jobTemplateId)
                    .append("; userId = ").append(account.geopal.userId)
                    .append("(maxoptra (host = ").append(account.maxoptra.host)
                    .append("; accountName = ").append(account.maxoptra.accountId)
            .append("; user = ").append(account.maxoptra.user)
            .append("; password = ").append(account.maxoptra.password).append("))");
        }
        return result.toString();
    }
}
