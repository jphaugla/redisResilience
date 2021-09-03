package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.domain.Customer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtil;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.stereotype.Repository;
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

	@Autowired
	@Qualifier("redisTemplate2")
	private RedisTemplate redisTemplate2;

    private RedisTemplate redisToUse;

	public CustomerRepository(@Qualifier("redisTemplate1") RedisTemplate redisTemplate) {
		redisToUse = redisTemplate1;
		logger.info("CustomerRepository constructor");
	}

	// @Retry(name = "backendA", fallbackMethod = "retryFallBack")
	@CircuitBreaker(name = "backendA", fallbackMethod = "cbFallBack")
	public String create(Customer customer)  {
		if(customer.getCreatedDatetime() == null ) {
			Long currentTimeMillis = System.currentTimeMillis();
			customer.setCreatedDatetime(currentTimeMillis);
			customer.setLastUpdated(currentTimeMillis);
		}
		Map<Object, Object> custHash = mapper.convertValue(customer, Map.class);
		if (redisToUse == null) redisToUse = redisTemplate1;
		redisToUse.opsForHash().putAll("Customer:"+ customer.getCustomerId(), custHash);
		logger.info(String.format("Customer with ID %s saved", customer.getCustomerId()));
		return "Success\n";
	}

	public String cbFallBack(Customer customer, Exception exception) {
		//  this gets called back with everyexception but only do the swith
		//  when it is called by the called not permitted exception (circuit breaker open)
		logger.info("cbFallBack call with exception " + exception.getMessage());
		if (exception instanceof CallNotPermittedException) {
			// toggle the redis template to use to failover
			if (redisToUse == redisTemplate1) {
				redisToUse = redisTemplate2;
				logger.info("Failed over from redistemplate1 to redistemplate2 ");
			} else {
				redisToUse = redisTemplate1;
				logger.info("Failed over from redistemplate2 to redistemplate1 ");
			}
		}
		return String.format("Fallback Execution for Circuit Breaker. Error Message: %s\n", exception.getMessage());
	}

	public String retryFallBack(Customer customer, Exception exception) {
		logger.info("retryFallBack call with exception " + exception.getMessage());
		return String.format("Fallback Execution for Retry. Error Message: %s\n", exception.getMessage());
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
