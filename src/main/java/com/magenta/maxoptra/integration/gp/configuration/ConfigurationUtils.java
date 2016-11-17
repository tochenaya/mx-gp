package com.magenta.maxoptra.integration.gp.configuration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ConfigurationUtils {

    private final static Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);

    protected static final String CONF_PROPERTY_NAME = "com.magenta.integration.geopal";
    protected static final String CONF_FILE_NAME = "mx-geopal.xml";
    public static final String geopalDateTimePattern = "yyyy-MM-dd HH:mm:ss";
    protected Configurations configurations;

    protected static ConcurrentHashMap<String, Account> accountsByMxAccountName; // maxoptra account name -> account config
    protected static ConcurrentHashMap<String, Account> accountsByGpTemplateId; // maxoptra account name -> account config

    @PostConstruct
    private void initConfiguration() throws Exception{
        readConfigurations();
    }

    public void readConfigurations() throws FileNotFoundException, JAXBException {
        log.info("Read configuration file");
        String confFilePath = getConfFilePath();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(confFilePath);
        } catch (FileNotFoundException ex) {
            log.warn("Configuration file not found in " + confFilePath);
            try {
                copyConfigurationFileFromResources(confFilePath);
                log.warn("Created default configuration file");
            } catch (IOException e) {
                log.error("Can't create configuration file", e);
            }
            inputStream = new FileInputStream(confFilePath);
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(Configurations.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        this.configurations = (Configurations) jaxbUnmarshaller.unmarshal(inputStream);
        fillAccountsMap();
    }

    protected void copyConfigurationFileFromResources(String confFilePath) throws IOException {
        URL inputUrl = getClass().getResource("/" + CONF_FILE_NAME);
        File dest = new File(confFilePath);
        FileUtils.copyURLToFile(inputUrl, dest);
    }

    protected String getConfFilePath() {
        String confPath = System.getProperty(CONF_PROPERTY_NAME);
        if (StringUtils.isNotBlank(confPath)) return confPath;

        String jbossConfFolder = System.getProperty("jboss.server.config.dir");
        confPath = jbossConfFolder + File.separator + CONF_FILE_NAME;
        return confPath;
    }

    public Configurations getConfigurations() throws FileNotFoundException, JAXBException {
        /*if (configurations == null) {
            readConfigurations();
        }*/
        return configurations;
    }

    public Account getAccountByMxAccountName(String accountName) throws FileNotFoundException, JAXBException {
        return accountsByMxAccountName.get(accountName);
    }

    public Account getAccountByGpTemplateId(String jobTemplateId) throws FileNotFoundException, JAXBException {
        return accountsByGpTemplateId.get(jobTemplateId);
    }

    public List<Account> getAccountList() throws FileNotFoundException, JAXBException {
        return getConfigurations().accounts.account;
    }

    protected void fillAccountsMap() throws FileNotFoundException, JAXBException {
        if (accountsByMxAccountName == null) accountsByMxAccountName = new ConcurrentHashMap<>();
        if (accountsByGpTemplateId == null) accountsByGpTemplateId = new ConcurrentHashMap<>();

        for (Account account : getConfigurations().accounts.account) {
            accountsByMxAccountName.put(account.maxoptra.accountId, account);
            accountsByGpTemplateId.put(account.geopal.jobTemplateId, account);
        }
    }

}
