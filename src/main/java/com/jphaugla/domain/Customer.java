package com.jphaugla.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@RedisHash("Customer")
public class Customer {
    private  @Id String customerId;
    private  String addressLine1;
    private  String addressLine2;
    private  String addressType;
    private  String billPayEnrolled;
    private  String city;
    private  String countryCode;
    private  String createdBy;
    private Date createdDatetime;
    private  String customerOriginSystem;
    private  String customerStatus;
    private  String customerType;
    private  String dateOfBirth;
    private  String firstName;
    private  String fullName;
    private  String gender;
    private  String governmentId;
    private  String governmentIdType;
    private  String lastName;
    private  Date lastUpdated;
    private  String lastUpdatedBy;
    private  String middleName;
    private  String prefix;
    private  String queryHelperColumn;
    private  String stateAbbreviation;
    private  String zipcode;
    private  String zipcode4;
    private  @Reference Email homeEmail;
    private  @Reference Email workEmail;
    private  @Reference Email customEmail1;
    private  @Reference Email customEmail2;
    private  @Reference PhoneNumber homePhone;
    private  @Reference PhoneNumber workPhone;
    private  @Reference PhoneNumber cellPhone;
    private  @Reference PhoneNumber customPhone;

    public List<Email> getCustomerEmails () {
        List<Email> emails = new ArrayList<Email>();
        if(homeEmail != null) emails.add(homeEmail);
        if(workEmail != null) emails.add(workEmail);
        if(customEmail1 != null) emails.add(customEmail1);
        if(customEmail2 != null) emails.add(customEmail2);
        return emails;
    }
    public List<PhoneNumber> getCustomerPhones () {
        List<PhoneNumber> phones = new ArrayList<PhoneNumber>();
        if(homePhone != null) phones.add(homePhone);
        if(workPhone != null) phones.add(workPhone);
        if(customPhone != null) phones.add(customPhone);
        return phones;
    }

}
