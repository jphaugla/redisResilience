package com.jphaugla.repository;

import com.jphaugla.domain.Customer;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CustomerRepository extends CrudRepository<Customer, String> {

	List<Customer> findByFirstNameAndLastName(String firstName, String lastName);

	List<Customer> findByStateAbbreviationAndCity(String stateAbbreviation, String city);

	List<Customer> findByzipcodeAndLastName(String zipcode, String lastName);

	List<Customer> findByMiddleNameContains(String firstName);

	List<Customer> findByRole_RoleName(String roleName);

}
