package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.domain.Customer;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Repository
public class CustomerRepository {
	private static final String KEY = "Customer";

	final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);

	@Autowired
	private RedisTemplateRepository redisTemplateRepository;

	ObjectMapper mapper = new ObjectMapper();

	public CustomerRepository() {
		logger.info("CustomerRepository constructor");
	}

	@Retry(name = "backendA")
	public String create(Customer customer) {
		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		Long millis = System.currentTimeMillis();
		Date date = new Date(millis);
		logger.info("in customerrepository.create with customerID " + customer.getCustomerId() + " " + date);
		logger.info("redis index is" + redisTemplateRepository.getRedisIndex());
		if (customer.getCreatedDatetime() == null) {
			customer.setCreatedDatetime(millis);
			customer.setLastUpdated(millis);
		}
		Map<Object, Customer> customerHash = mapper.convertValue(customer, Map.class);
		redisTemplateRepository.getWriteTemplate().opsForHash().putAll("Customer:" + customer.getCustomerId(), customerHash);
		logger.info(String.format("Customer with ID %s saved", customer.getCustomerId()));
		return "Success\n";
	}

	@Retry(name = "backendA")
	public Customer get(String customerId) {
		logger.info("in CustomerRepository.get with customer id=" + customerId);
		logger.info("redis index is" + redisTemplateRepository.getRedisIndex());
		String fullKey = "Customer:" + customerId;
		Map<Object, Object> customerHash = redisTemplateRepository.getReadTemplate().opsForHash().entries(fullKey);
		// Map<Object, Object> customerHash = redisTemplate1.opsForHash().entries(fullKey);
		Customer customer = mapper.convertValue(customerHash, Customer.class);
		return (customer);
	}

}