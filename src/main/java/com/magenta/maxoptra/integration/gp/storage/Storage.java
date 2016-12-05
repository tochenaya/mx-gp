package com.magenta.maxoptra.integration.gp.storage;

import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.pojo.webhook.ChangeOrderStatusRecord;
import com.magenta.maxoptra.integration.gp.storage.Connection.WrapperConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Storage {

    @Inject
    ConfigurationUtils configurationUtils;

    @Inject
    private WrapperConnection wrapperConnection;

    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    protected String getCreateTableSQL() throws Exception {
        return "CREATE TABLE `" + getTableName() + "` (`id` BIGINT(20) NOT NULL AUTO_INCREMENT," +
                "`account` VARCHAR(250) NULL DEFAULT NULL,`reference` VARCHAR(250) NULL DEFAULT NULL," +
                "`oldStatus` VARCHAR(250) NULL DEFAULT NULL,`newStatus` VARCHAR(250) NULL DEFAULT NULL," +
                "`engineer` VARCHAR(250) NULL DEFAULT NULL,`engineerExternalId` VARCHAR(250) NULL DEFAULT NULL," +
                "`allocationDate` VARCHAR(250) NULL DEFAULT NULL,`eventDateTime` VARCHAR(250) NULL DEFAULT NULL," +
                "`jobStartTime` VARCHAR(250) NULL DEFAULT NULL, PRIMARY KEY (`id`))";
    }

    @PostConstruct
    public void init() throws Exception {
        log.info("Start initialize database");
        if (!isTableExists()) {
            initDataBase();
        }
    }

    public boolean isEnable() {
        return wrapperConnection != null && wrapperConnection.getConnection() != null;
    }

    public Connection getConnection() {
        return wrapperConnection.getConnection();
    }

    public void initDataBase() throws Exception {
        try {
            log.info("Create database table structure");
            PreparedStatement ps = getConnection().prepareStatement(getCreateTableSQL());
            ps.execute();
            ps.close();
        } catch (Exception ex) {
            throw new Exception("Error when try to create database table structure", ex);
        }
    }

    public boolean isTableExists() {
        try {
            PreparedStatement ps = getConnection().prepareStatement("select count(*) from " + getTableName() + ";");
            ResultSet rs = ps.executeQuery();
            rs.next();
            log.info("Table exists");
            rs.close();
            ps.close();
            return true;
        } catch (Exception e) {
            //Если такблица не будет найдена, тогда будет исключение. Значит старой информации нет.
        }
        log.info("Table do not exists");
        return false;
    }

    public void add(ChangeOrderStatusRecord record) throws Exception {
        PreparedStatement ps = getConnection().prepareStatement("insert into " + getTableName() + " (account, reference, oldStatus, newStatus, engineer, engineerExternalId, allocationDate, eventDateTime, jobStartTime) values (?, ?, ?, ?, ?, ?, ?, ?, ?);");
        ps.setString(1, record.account);
        ps.setString(2, record.reference);
        ps.setString(3, record.oldStatus);
        ps.setString(4, record.newStatus);
        ps.setString(5, record.engineer);
        ps.setString(6, record.engineerExternalId);
        ps.setString(7, record.allocationDate);
        ps.setString(8, record.eventDateTime);
        ps.setString(9, record.jobStartTime);
        ps.execute();
        ps.close();
    }

    public ArrayList<ChangeOrderStatusRecord> getState() throws Exception {
        ArrayList<ChangeOrderStatusRecord> list = new ArrayList<>();
        String query = "select * from " + getTableName();
        PreparedStatement ps = getConnection().prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(parseResultSet(rs));
        }
        rs.close();
        ps.close();
        return list;
    }

    private ChangeOrderStatusRecord parseResultSet(ResultSet rs) throws SQLException {
        ChangeOrderStatusRecord record = new ChangeOrderStatusRecord();
        record.id = rs.getInt(1);
        record.account = rs.getString(2);
        record.reference = rs.getString(3);
        record.oldStatus = rs.getString(4);
        record.newStatus = rs.getString(5);
        record.engineer = rs.getString(6);
        record.engineerExternalId = rs.getString(7);
        record.allocationDate = rs.getString(8);
        record.eventDateTime = rs.getString(9);
        record.jobStartTime = rs.getString(10);
        return record;
    }

    public ArrayList<ChangeOrderStatusRecord> getStateByAccount(Account account) throws Exception {
        ArrayList<ChangeOrderStatusRecord> list = new ArrayList<>();
        String query = "select * from " + getTableName() + " o where o.account = ?";
        PreparedStatement ps = getConnection().prepareStatement(query);
        ps.setString(1, account.maxoptra.accountId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(parseResultSet(rs));
        }
        rs.close();
        ps.close();
        return list;
    }

    public void removeSelectedRecords(ArrayList<ChangeOrderStatusRecord> orders) throws Exception {
        log.info("Run clear selected records");
        PreparedStatement ps = getConnection().prepareStatement("delete from " + getTableName() + " WHERE id = ?");
        for (ChangeOrderStatusRecord record : orders) {
            ps.setInt(1, record.id);
            ps.execute();
        }
        ps.close();
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        try {
            HashMap<String, List<String>> hl = new HashMap<>();
            int qSize = getState().size();
            if (qSize == 0) {
                if (sb.length() == 0) sb.append("Storage is empty.");
            } else {
                sb.append("Storage size ").append(qSize).append(":<br>");
                for (ChangeOrderStatusRecord record : getState()) {
                    List<String> l = hl.get(record.account);
                    if (l == null) l = new ArrayList<>();
                    l.add("<span style='margin-left:25px;'>" + record.reference + " from " + record.oldStatus + " to " + record.newStatus + "</span><br>");
                    hl.put(record.account, l);
                }
                for (Map.Entry<String, List<String>> entry : hl.entrySet()) {
                    sb.append("<h4 style='margin-bottom: 10px;'>Account : ").append(entry.getKey()).append("</h4>");
                    for (String s : entry.getValue()) {
                        sb.append(s);
                    }
                    sb.append("<br>");
                }
            }
        } catch (Exception ex) {
            log.error("Error when try to get storage status:", ex);
        }
        return sb.toString();
    }

    public void executeSQL(String sql) throws Exception {
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.executeUpdate();
        ps.close();
    }

    public String getTableName() throws FileNotFoundException, JAXBException {
        return configurationUtils.getConfigurations().storageTableName;
    }

}
