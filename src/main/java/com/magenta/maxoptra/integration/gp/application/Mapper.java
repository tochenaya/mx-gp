package com.magenta.maxoptra.integration.gp.application;

import com.magenta.maxoptra.integration.commons.orders.requests.OrderSaveRequest;
import com.magenta.maxoptra.integration.gp.configuration.Account;
import com.magenta.maxoptra.integration.gp.configuration.ConfigurationUtils;
import com.magenta.maxoptra.integration.gp.configuration.JobFieldsConf;
import com.magenta.maxoptra.integration.gp.connection.http.email.EmailService;
import com.magenta.maxoptra.integration.gp.connection.http.exeption.GPErrorException;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Asset;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Customer;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.JobData;
import com.magenta.maxoptra.integration.gp.pojo.geopalRecord.Person;
import com.magenta.maxoptra.integration.gp.pojo.maxoptraRecord.Performer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Mapper {

    @Inject
    private ConfigurationUtils configurationUtils;

    @Inject
    private PerformerServices performerServices;

    @EJB
    private EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(Mapper.class);

    private static final String JOB_FIELD_TYPE_LIST_MULTIPLE = "listmultiple";
    private static final String JOB_FIELD_TYPE_LIST = "list";
    private static final String JOB_FIELD_TYPE_TEXT = "text";

    public List<OrderSaveRequest.Orders.Order> gpToMxOrder(Account account, JobData jobData, Asset asset) throws Exception {

        //кастомер у ассета тот же что и у джоба(регулируется геопалом)
        //asset.setCustomer(jobData.getCustomer());

        List<OrderSaveRequest.Orders.Order> ordersToAdd = new ArrayList<>();
        OrderSaveRequest.Orders.Order order = new OrderSaveRequest.Orders.Order();

        order.setOrderReference(jobData.getIdentifier() + "/" + jobData.getId());
        order.setDurationDrop(fromSecToTime(jobData.getEstimated_duration()));

        order.setDueDate(dateFromGpToMx(jobData.getPrefered_stop_date_time(), account));
        order.setReleaseDate(dateFromGpToMx(jobData.getPrefered_start_date_time(), account));

        //order.setClient(gpToMxClient(jobData.getCustomer(), jobData.getPerson())); сказали client устаревший, не нужен
        order.setCustomerLocation(gpToMxCustomerLocation(asset, jobData));

        order.setAreaOfControl(account.maxoptra.aoc);
        order.setPriority(gpToMxPriority(jobData.getPriority_text() == null ? "" : jobData.getPriority_text()));
        order.setWorkDescription(jobData.getNotes().replaceAll("\\<.*?>", ""));
        gpToMxTemplateField(account, order, jobData);

        ordersToAdd.add(order);
        return ordersToAdd;
    }

    private void gpToMxTemplateField(Account account, OrderSaveRequest.Orders.Order order, JobData gpJobData) throws Exception {

        List<JobFieldsConf.JobFieldConf> jobFieldsConf = account.jobFieldsConf.jobFieldConfs;
        List<JobData.JobField> jobFields = gpJobData.getJobFields();

        for (JobFieldsConf.JobFieldConf jobFieldConf : jobFieldsConf) {
            Optional<JobData.JobField> jobFieldOptional = jobFields.stream().filter(e -> e.getTemplate_field_id().equals(jobFieldConf.gpFieldId)).findFirst();
            if (!jobFieldOptional.isPresent()) continue; //пришло кастомное поле которого нету в нашем конфиге
            JobData.JobField jobField = jobFieldOptional.get();
            if (StringUtils.isBlank(jobField.getAction_value_entered())) continue; //кастомное поле пустое

            switch (jobFieldConf.mxFieldName) {
                case "Job_types":
                    OrderSaveRequest.Orders.Order.JobTypes jobTypes = new OrderSaveRequest.Orders.Order.JobTypes();
                    if (jobField.getAction().equalsIgnoreCase(JOB_FIELD_TYPE_LIST_MULTIPLE) ||
                            jobField.getAction().equalsIgnoreCase(JOB_FIELD_TYPE_LIST) ||
                            jobField.getAction().equalsIgnoreCase(JOB_FIELD_TYPE_TEXT)) {
                        for (String value : jobField.getAction_value_entered().split(",")) {
                            OrderSaveRequest.Orders.Order.JobTypes.JobType jobType = new OrderSaveRequest.Orders.Order.JobTypes.JobType();
                            jobType.setName(value);
                            jobTypes.getJobType().add(jobType);
                        }
                    }
                    order.setJobTypes(jobTypes);
                    break;
                case "Skills":
                    OrderSaveRequest.Orders.Order.Skills skills = new OrderSaveRequest.Orders.Order.Skills();
                    if (jobField.getAction().equalsIgnoreCase(JOB_FIELD_TYPE_LIST_MULTIPLE) ||
                            jobField.getAction().equalsIgnoreCase(JOB_FIELD_TYPE_LIST) ||
                            jobField.getAction().equalsIgnoreCase(JOB_FIELD_TYPE_TEXT)) {
                        for (String value : jobField.getAction_value_entered().split(",")) {
                            OrderSaveRequest.Orders.Order.Skills.Skill skill = new OrderSaveRequest.Orders.Order.Skills.Skill();
                            skill.setName(value);
                            skills.getSkill().add(skill);
                        }
                    }
                    order.setSkills(skills);
                    break;
                case "includeList":
                    OrderSaveRequest.Orders.Order.IncludeList includeList = new OrderSaveRequest.Orders.Order.IncludeList();
                    String driverName = jobField.getAction_value_entered();
                    Performer performer = null;
                    try {
                        performer = performerServices.getMXPerformerByName(account, driverName);
                    } catch (Exception e) {
                        String massage = e.getMessage() + " Preferred Engineer for " + gpJobData.getIdentifier() + "/" + gpJobData.getId() +
                                " will not be specified in Maxoptra.";
                        log.warn(massage);
                        emailService.sendErrors(account, massage);
                        break;
                    }
                    String driverExternalId = performer.getExternalId();
                    includeList.getDriver().add(driverExternalId);
                    order.setIncludeList(includeList);
                    break;
            }
        }
    }

    private OrderSaveRequest.Orders.Order.CustomerLocation gpToMxCustomerLocation(Asset asset, JobData jobData) throws Exception {
        Person person = jobData.getPerson();

        OrderSaveRequest.Orders.Order.CustomerLocation customerLocation = new OrderSaveRequest.Orders.Order.CustomerLocation();
        OrderSaveRequest.Orders.Order.CustomerLocation.Position position = new OrderSaveRequest.Orders.Order.CustomerLocation.Position();
        OrderSaveRequest.Orders.Order.CustomerLocation.Customer customer = new OrderSaveRequest.Orders.Order.CustomerLocation.Customer();

        if (person.getId() != null) {
            String first_name = StringUtils.isNoneBlank(person.getFirst_name()) ? person.getFirst_name() : "";
            String last_name = StringUtils.isNoneBlank(person.getLast_name()) ? person.getLast_name() : "";
            customerLocation.setContactName(first_name + " " + last_name);
            customer.setContactPerson(first_name + " " + last_name);
        }

        if (jobData.getCustomer().getId() != null) {
            customerLocation.setPhoneNumber(StringUtils.isNoneBlank(jobData.getCustomer().getPhone_office()) ? jobData.getCustomer().getPhone_office() : "");
            customerLocation.setEmail(StringUtils.isNoneBlank(jobData.getCustomer().getEmail()) ? jobData.getCustomer().getEmail() : "");

            customer.setName(StringUtils.isNoneBlank(jobData.getCustomer().getName()) ? jobData.getCustomer().getName() : "");
            customer.setContactNumber(StringUtils.isNoneBlank(jobData.getCustomer().getPhone_office()) ? jobData.getCustomer().getPhone_office() : "");
        }

        if (asset != null && asset.getAddress() != null && StringUtils.isNotBlank(asset.getAddress().getId())) {
            customerLocation.setName(asset.getName());

            StringBuilder address = new StringBuilder();
            if(StringUtils.isNoneBlank(asset.getAddress().getAddressLine1())){
                address.append(asset.getAddress().getAddressLine1());
                address.append(", ");
            }
            if(StringUtils.isNoneBlank(asset.getAddress().getAddressLine2())){
                address.append(asset.getAddress().getAddressLine2());
                address.append(", ");
            }
            if(StringUtils.isNoneBlank(asset.getAddress().getAddressLine3())){
                address.append(asset.getAddress().getAddressLine3());
                address.append(", ");
            }
            if(StringUtils.isNoneBlank(asset.getAddress().getPostalCode())){
                address.append(asset.getAddress().getPostalCode());
                address.append(", ");
            }
            String addressStr = address.substring(0, address.length() - 2);
            position.setAddress(addressStr);

            position.setLatitude(asset.getAddress().getLat().equals(0f) ? null : asset.getAddress().getLat().toString());
            position.setLongitude(asset.getAddress().getLng().equals(0f) ? null : asset.getAddress().getLng().toString());
            customerLocation.setPosition(position);

        } else if (StringUtils.isNotBlank(jobData.getAddress().getAddressLine1()) ||
                StringUtils.isNotBlank(jobData.getAddress().getAddressLine2()) ||
                StringUtils.isNotBlank(jobData.getAddress().getAddressLine3())) {

            StringBuilder address = new StringBuilder();
            if(StringUtils.isNoneBlank(jobData.getAddress().getAddressLine1())){
                address.append(jobData.getAddress().getAddressLine1());
                address.append(", ");
            }
            if(StringUtils.isNoneBlank(jobData.getAddress().getAddressLine2())){
                address.append(jobData.getAddress().getAddressLine2());
                address.append(", ");
            }
            if(StringUtils.isNoneBlank(jobData.getAddress().getAddressLine3())){
                address.append(jobData.getAddress().getAddressLine3());
                address.append(", ");
            }
            if(StringUtils.isNoneBlank(jobData.getAddress().getPostalCode())){
                address.append(jobData.getAddress().getPostalCode());
                address.append(", ");
            }
            String addressStr = address.substring(0, address.length() - 2);
            position.setAddress(addressStr);
            customerLocation.setName(addressStr);

            position.setLatitude(jobData.getAddress().getLat().equals(0f) ? null : jobData.getAddress().getLat().toString());
            position.setLongitude(jobData.getAddress().getLng().equals(0f) ? null : jobData.getAddress().getLng().toString());
            customerLocation.setPosition(position);
        } else {
            String massage = "GeoPal data is incomplete. Job " + jobData.getIdentifier() + "/" + jobData.getId() + " has no location details and was not imported to Maxoptra.";
            throw new GPErrorException(massage);
        }

        customerLocation.setCustomer(customer);

        return customerLocation;
    }

    public OrderSaveRequest.Orders.Order.Client gpToMxClient(Customer customer, Person person) {
        OrderSaveRequest.Orders.Order.Client client = new OrderSaveRequest.Orders.Order.Client();
        if (StringUtils.isNotBlank(customer.getName())) {
            client.setName(customer.getName());
        }
        if (StringUtils.isNotBlank(person.getFirst_name()) && StringUtils.isNotBlank(person.getLast_name())) {
            client.setContactPerson(person.getFirst_name() + " " + person.getLast_name());
        }
        if (StringUtils.isNotBlank(person.getPhone_number())) {
            client.setContactNumber(person.getPhone_number());
        }
        return client;
    }

    public String gpToMxPriority(String priority) {
        priority = StringUtils.trim(priority);
        if (priority.equalsIgnoreCase("Low"))
            return "1";
        if (priority.equalsIgnoreCase("Normal"))
            return "2";
        if (priority.equalsIgnoreCase("High"))
            return "3";
        return "2"; //default Normal
    }

    public String dateFromGpToMx(String gpDate, Account account) throws Exception {
        return dateConvert(gpDate, ConfigurationUtils.geopalDateTimePattern, account.maxoptra.dateTimePattern);
    }

    public String dateFromMxToGp(String gpDate, Account account) throws Exception {
        return dateConvert(gpDate, account.maxoptra.dateTimePattern, ConfigurationUtils.geopalDateTimePattern);
    }

    public String dateConvert(String dateStr, String patternFrom, String patternTo) throws Exception {
        if (dateStr.equals("0000-00-00 00:00:00") || dateStr.equals("0000-00-00") ||
                dateStr.equals("00:00:00") || dateStr.isEmpty()) return "";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patternFrom);
        Date date = simpleDateFormat.parse(dateStr);

        simpleDateFormat.applyPattern(patternTo);
        return simpleDateFormat.format(date);
    }

    private String fromSecToTime(Integer seconds) {
        Date date = new Date(seconds * 1000);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(date);
    }
}
