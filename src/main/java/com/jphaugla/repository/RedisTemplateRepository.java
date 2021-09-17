package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.service.ChooseRedis;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.lang.Thread.*;

import java.time.Duration;


@Repository
public class RedisTemplateRepository {

	final Logger logger = LoggerFactory.getLogger(RedisTemplateRepository.class);

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private ChooseRedis chooseRedis;

	@Autowired
	@Qualifier("redisTemplateWriteArray")
	private RedisTemplate [] redisTemplateWriteArray;

	@Autowired
	@Qualifier("redisTemplateReadArray")
	private StringRedisTemplate[] redisTemplateReadArray;

	@Autowired
	@Qualifier("redisConnectionFactory1")
	private RedisConnectionFactory redisConnectionFactory1;

	@Autowired
	@Qualifier("redisConnectionFactory2")
	private RedisConnectionFactory redisConnectionFactory2;

	private String key1 = "key1";
	private String key2 = "key2";


	public RedisTemplateRepository() {
		logger.info("RedisTemplateRepository constructor");
	}

	public void setKeys () {
		redisTemplateReadArray[0].opsForValue().set(key1, key1);
		redisTemplateReadArray[1].opsForValue().set(key2, key2);
	}

	@CircuitBreaker(name = "zCircuitBreaker", fallbackMethod = "switchTemplate")
	public Boolean testTheWrite(String stringKey, String stringValue )  {
		redisTemplateReadArray[chooseRedis.getRedisIndex()].opsForValue().set(stringKey, stringValue);
		// logger.info ("after call to set the value");
		String returnValue = (String) redisTemplateReadArray[chooseRedis.getRedisIndex()].opsForValue().get(stringKey);
		boolean b = false;
		return (b);
	}

	public Boolean switchTemplate(String stringKey, String stringValue , Exception exception) throws InterruptedException {
		//  this gets called back with every exception but only  do the switch
		//  when it is called by the called not permitted exception (circuit breaker open)
		String exceptionMessage = "";
		if(exception == null) {
			exceptionMessage = "exception is Null";
		} else {
			exceptionMessage = exception.getMessage();
		}
		logger.info("switchtemplate with exception " + exceptionMessage);
		boolean returnFailedOver = false;
		if ( exception == null || exception instanceof CallNotPermittedException ) {
			// toggle the redis template to use to failover
			if (chooseRedis.getRedisIndex() == 0) {
				chooseRedis.setRedisIndex(1);
				logger.info("Failed over from redistemplate1 to redistemplate2 redisIndex is " + chooseRedis.getRedisIndex());
			} else {
				chooseRedis.setRedisIndex(0);
				logger.info("Failed over from redistemplate2 to redistemplate1 redisIndex is " + chooseRedis.getRedisIndex());
			}
			returnFailedOver = true;
		}
		logger.info("Failover is " + returnFailedOver + " redisIndex is " + chooseRedis.getRedisIndex());
		return returnFailedOver;
	}

	private String getFromConnectionFactory(RedisConnectionFactory redisConnectionFactory, String key) {
		RedisConnection connection = redisConnectionFactory.getConnection();
		logger.info("Retrieving using connection from " + redisConnectionFactory);
		byte[] response = connection.get(key.getBytes());
		connection.close();
		return response == null ? null : new String(response);
	}

	//  this version tries to check to see if the database to failover to is up before doing the failover but it did not work so going back to simpler
	public Boolean oldSwitchTemplate(String stringKey, String stringValue , Exception exception) throws InterruptedException {
		//  this gets called back with every exception but only  do the switch
		//  when it is called by the called not permitted exception (circuit breaker open)
		//  check to make sure target redis is up before making the switch
		logger.info("switchtemplate with exception " + exception.getMessage());
		boolean returnFailedOver = false;
		if (exception instanceof CallNotPermittedException) {
			// toggle the redis template to use to failover
			if (chooseRedis.getRedisIndex() == 0) {
				String returnValue = getFromConnectionFactory(redisConnectionFactory2,"key2");
				logger.info("retrunValue from key2 is " + returnValue);
				if ( returnValue != null) {
					chooseRedis.setRedisIndex(1);
					returnFailedOver = true;
					logger.info("Failed over from redistemplate1 to redistemplate2 redisIndex is " + chooseRedis.getRedisIndex());
				} else {
					logger.info("Did not fail over as redis2 is down");
				}
			} else {
				String returnValue = getFromConnectionFactory(redisConnectionFactory1,"key1");
				logger.info("returnValue from key1 is " + returnValue);
				if (getFromConnectionFactory(redisConnectionFactory1,"key1") != null) {
					chooseRedis.setRedisIndex(0);
					returnFailedOver = true;
					logger.info("Failed over from redistemplate2 to redistemplate1 redisIndex is " + chooseRedis.getRedisIndex());
				} else {
					logger.info("Did not fail over as redis2 is down");
				}
			}
		}
		logger.info("Failover is " + returnFailedOver + " redisIndex is " + chooseRedis.getRedisIndex());
		return returnFailedOver;
	}


}
