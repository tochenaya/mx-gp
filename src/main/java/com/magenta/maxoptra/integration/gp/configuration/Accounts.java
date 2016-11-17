package com.magenta.maxoptra.integration.gp.configuration;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class Accounts {
    @XmlElement(required = true)
    public List<Account> account;
}
