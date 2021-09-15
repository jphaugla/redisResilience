package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.domain.Customer;
import com.jphaugla.service.ChooseRedis;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class CustomerRepository {
	private static final String KEY = "Customer";


	final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);

	@Autowired
	private ChooseRedis chooseRedis;


	@Autowired
	@Qualifier("redisTemplateWriteArray")
	private RedisTemplate[] redisTemplateWriteArray;

	@Autowired
	@Qualifier("redisTemplateReadArray")
	private RedisTemplate[] redisTemplateReadArray;

	ObjectMapper mapper = new ObjectMapper();

	public CustomerRepository() {
		logger.info("CustomerRepository constructor");
	}

	@CircuitBreaker(name = "backendA", fallbackMethod = "createCallBack")
	public String create(Customer customer) {
		if (customer.getCreatedDatetime() == null) {
			Long currentTimeMillis = System.currentTimeMillis();
			customer.setCreatedDatetime(currentTimeMillis);
			customer.setLastUpdated(currentTimeMillis);
		}
		Map<Object, Customer> customerHash = mapper.convertValue(customer, Map.class);
		redisTemplateWriteArray[chooseRedis.getRedisIndex()].opsForHash().putAll("Customer:" + customer.getCustomerId(), customerHash);
		logger.info(String.format("Customer with ID %s saved", customer.getCustomerId()));
		return "Success\n";
	}

	@CircuitBreaker(name = "backendA", fallbackMethod = "getCallBack")
	public Customer get(String customerId) {
		logger.info("in CustomerRepository.get with customer id=" + customerId);
		String fullKey = "Customer:" + customerId;
		Map<Object, Customer> customerHash = redisTemplateReadArray[chooseRedis.getRedisIndex()].opsForHash().entries(fullKey);
		// Map<Object, Object> customerHash = redisTemplate1.opsForHash().entries(fullKey);
		Customer customer = mapper.convertValue(customerHash, Customer.class);
		return (customer);
	}

	public String createCallBack(Customer customer, Exception exception) {
		//  this gets called back with every exception but only do the switch
		//  when it is called by the called not permitted exception (circuit breaker open)
		logger.info("createCallBack with exception " + exception.getMessage());
		return exception.getMessage();
	}
	public Customer getCallBack(String customerId, Exception exception) {
		//  this gets called back with every exception but only do the switch
		//  when it is called by the called not permitted exception (circuit breaker open)
		logger.info("getCallBack with exception " + exception.getMessage());
		return (null);
	}
}