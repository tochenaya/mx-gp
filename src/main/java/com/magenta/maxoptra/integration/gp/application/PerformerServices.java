package com.magenta.maxoptra.integration.gp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magenta.maxoptra.integration.api.MXApiClient;
import com.magenta.maxoptra.integration.commons.orders.ExportPerformers;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.connection.http.HttpService;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.GPErrorException;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Employee;
import com.magenta.maxoptra.integration.gp.pojo.maxoptraRecord.Performer;
import com.magenta.maxoptra.integration.gp.pojo.response.EmployeesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class PerformerServices {

    private static final Logger log = LoggerFactory.getLogger(PerformerServices.class);

    @Inject
    private MxApiClientFactory mxApiClientFactory;

    @Inject
    private HttpService httpService;

    protected ConcurrentMap<String, ConcurrentMap<String, Performer>> mapMXPerformers = new ConcurrentHashMap<>(); //accountName  -> performerName -> performer
    protected ConcurrentMap<String, ConcurrentMap<String, Employee>> mapGPEmployees = new ConcurrentHashMap<>(); //accountName  -> employeeName -> employee

    public Performer getMXPerformerByName(Account account, String performerName) throws Exception {
        ConcurrentMap<String, Performer> namePerformerMap = mapMXPerformers.get(account.maxoptra.accountId);
        if (namePerformerMap == null) {
            namePerformerMap = new ConcurrentHashMap<>();
            MXApiClient mxApiClient = mxApiClientFactory.getMxApiClient(account);
            List<ExportPerformers.PerformersExport.Performers.Performer> performers = null;
            try {
                performers = mxApiClient.exportPerformers(Long.getLong(account.maxoptra.aoc)).getPerformersExport().getPerformers().getPerformer();
            } catch (NullPointerException e) {
                throw new GPErrorException("No engineers found in Maxoptra for " + account.maxoptra.aoc);
            }
            for (ExportPerformers.PerformersExport.Performers.Performer performer : performers) {
                Performer performerRecord = new Performer();
                performerRecord.setExternalId(performer.getExternalId());
                performerRecord.setName(performer.getName());
                namePerformerMap.put(performer.getName().toLowerCase(), performerRecord);
            }

            mapMXPerformers.put(account.maxoptra.accountId, namePerformerMap);
        }
        Performer performer = namePerformerMap.get(performerName.toLowerCase());
        if (performer == null) {
            throw new GPErrorException("Engineer " + performerName + " was not found in Maxoptra.");
        }
        return performer;
    }

    public Employee getGPEmployeeByName(Account account, String employeeName) throws Exception {
        ConcurrentMap<String, Employee> nameEmployeeMap = mapGPEmployees.get(account.maxoptra.accountId);
        if(nameEmployeeMap == null) {
            nameEmployeeMap = new ConcurrentHashMap<>();

            String employeesStr = httpService.get("api/employees/all", null, account.geopal);
            ObjectMapper objectMapper = new ObjectMapper();
            EmployeesResponse employeesResponse = objectMapper.readValue(employeesStr, EmployeesResponse.class);
            log.info("employeesResponse = " + employeesResponse.toString());
            if (employeesResponse.getErrorCode() != null) {
                throw new GPErrorException("Error has occurred during getting employees in GeoPal " +
                        employeesResponse.getErrorCode() + ": " + employeesResponse.getErrorMessage());
            }

            List<Employee> employees = employeesResponse.getEmployees();
            for(Employee employee: employees){
                nameEmployeeMap.put(employee.getFirst_name() + " " + employee.getLast_name(), employee);
            }

            mapGPEmployees.put(account.maxoptra.accountId, nameEmployeeMap);
        }
        Employee employee = nameEmployeeMap.get(employeeName);
        if(employee == null){
            throw new GPErrorException("Engineer " + employeeName + " was not found in Geopal.");
        }
        return employee;
    }

    public void clearMap(){
        mapMXPerformers.clear();
        mapGPEmployees.clear();
    }
}
