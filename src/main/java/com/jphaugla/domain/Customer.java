package com.jphaugla.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Customer implements Serializable {
    private  String customerId;
    private  String addressLine1;
    private  String addressLine2;
    private  String addressType;
    private  String billPayEnrolled;
    private  String city;
    private  String countryCode;
    private  String createdBy;
    private Long createdDatetime;
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
    private  Long lastUpdated;
    private  String lastUpdatedBy;
    private  String middleName;
    private  String prefix;
    private  String queryHelperColumn;
    private  String stateAbbreviation;
    private  String zipcode;
    private  String zipcode4;


}
