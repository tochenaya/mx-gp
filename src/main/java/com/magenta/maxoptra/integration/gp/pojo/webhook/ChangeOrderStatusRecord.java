package com.magenta.maxoptra.integration.gp.pojo.webhook;

import com.magenta.maxoptra.integration.gp.pojo.webhook.UnitsRecord;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "OrderStatusChange")
 public class ChangeOrderStatusRecord {

    @XmlTransient
    public int id;
    public UnitsRecord units;
    public String account;
    public String reference;
    public String oldStatus;
    public String newStatus;
    public String engineer;
    public String engineerExternalId;
    public String allocationDate;
    public String eventDateTime;
    public String jobStartTime;

    @Override
    public String toString() {
        return "ChangeOrderStatusRecord{" +
                "id=" + id +
                ", units=" + units +
                ", account='" + account + '\'' +
                ", reference='" + reference + '\'' +
                ", oldStatus='" + oldStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", engineer='" + engineer + '\'' +
                ", engineerExternalId='" + engineerExternalId + '\'' +
                ", allocationDate='" + allocationDate + '\'' +
                ", eventDateTime='" + eventDateTime + '\'' +
                ", jobStartTime='" + jobStartTime + '\'' +
                '}';
    }
}
