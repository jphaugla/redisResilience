package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.domain.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;


import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CustomerRepository  {
	private static final String KEY = "Customer";

	final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);

	private HashOperations<String, String, Customer> hashOperations;
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	@Qualifier("redisTemplate1")
	private RedisTemplate redisTemplate1;

	public CustomerRepository(@Qualifier("redisTemplate1") RedisTemplate redisTemplate) {
		this.hashOperations = redisTemplate.opsForHash();
	}

	public void create(Customer customer) {
		Map<Object, Object> custHash = mapper.convertValue(customer, Map.class);
		redisTemplate1.opsForHash().putAll("Customer:"+ customer.getCustomerId(), custHash);
		logger.info(String.format("Customer with ID %s saved", customer.getCustomerId()));
	}

	public Customer get(String customerId) {
		logger.info("in get customer with customer id=" + customerId);
		String fullKey = "Customer:" + customerId;

		Map<Object, Object> custHash = redisTemplate1.opsForHash().entries(fullKey);
		Customer customer = mapper.convertValue(custHash,Customer.class);
		return (customer);
	}

	public void delete(String customerId) {
		String fullKey = "Customer:" + customerId;
		redisTemplate1.delete(fullKey);
		logger.info(String.format("Customer with ID %s deleted", customerId));
	}

}
