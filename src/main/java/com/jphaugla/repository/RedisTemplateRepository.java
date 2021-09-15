package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.service.ChooseRedis;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
	private RedisTemplate [] redisTemplateReadArray;



	public RedisTemplateRepository() {
		logger.info("RedisTemplateRepository constructor");
	}

	@CircuitBreaker(name = "backendA", fallbackMethod = "switchTemplate")
	public Boolean testTheWrite(String stringKey, String stringValue )  {
		redisTemplateReadArray[chooseRedis.getRedisIndex()].opsForValue().set(stringKey, stringValue);
		// logger.info ("after call to set the value");
		String returnValue = (String) redisTemplateReadArray[chooseRedis.getRedisIndex()].opsForValue().get(stringKey);
		boolean b = false;
		return (b);
	}

	public Boolean switchTemplate(String stringKey, String stringValue , Exception exception) throws InterruptedException {
		//  this gets called back with every exception but only do the switch
		//  when it is called by the called not permitted exception (circuit breaker open)
		logger.info("switchtemplate with exception " + exception.getMessage());
		boolean returnFailedOver = false;
		if (exception instanceof CallNotPermittedException) {
			// toggle the redis template to use to failover
			if (chooseRedis.getRedisIndex() == 0) {
				chooseRedis.setRedisIndex(1);
				logger.info("Failed over from redistemplate1 to redistemplate2 redisIndex is " + chooseRedis.getRedisIndex());
			} else {
				chooseRedis.setRedisIndex(0);
				logger.info("Failed over from redistemplate2 to redistemplate1 redisIndex is "  + chooseRedis.getRedisIndex());
			}
			returnFailedOver = true;
		}
		logger.info("Failover is " + returnFailedOver + " redisIndex is " + chooseRedis.getRedisIndex());
		return returnFailedOver;
	}

}
