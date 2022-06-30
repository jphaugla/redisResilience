package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Repository;


@Repository
@Configuration
public class RedisTemplateRepository {

	final Logger logger = LoggerFactory.getLogger(RedisTemplateRepository.class);

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private Environment env;

	@Autowired
	@Qualifier("redisConnectionFactory1")
	private LettuceConnectionFactory redisConnectionFactory1;

	@Autowired
	@Qualifier("redisConnectionFactory2")
	private LettuceConnectionFactory redisConnectionFactory2;

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;

	@Autowired
	@Qualifier("redisTemplateW2")
	private RedisTemplate<Object, Object> redisTemplateW2;

	@Autowired
	@Qualifier("stringRedisTemplate1")
	private StringRedisTemplate stringRedisTemplate1;

	@Autowired
	@Qualifier("stringRedisTemplate2")
	private StringRedisTemplate stringRedisTemplate2;

	private RedisTemplate<Object, Object>[] redisTemplateWriteArray;
	private StringRedisTemplate[] redisTemplateReadArray;

	//  redisIndex is which is the currently active redis connection
	private int redisIndex;

	private String key1 = "key1";
	private String key2 = "key2";

	public RedisTemplateRepository(@Qualifier("redisTemplateW1") RedisTemplate redisTemplateW1, @Qualifier("redisTemplateW2") RedisTemplate redisTemplateW2,
								   @Qualifier("stringRedisTemplate1") StringRedisTemplate stringRedisTemplate1,  @Qualifier("stringRedisTemplate2") StringRedisTemplate stringRedisTemplate2) {
		logger.info("starting RedisTemplateRepository constructor");
		redisIndex = 0;
		redisTemplateWriteArray = new RedisTemplate[]{redisTemplateW1, redisTemplateW2};
		redisTemplateReadArray = new StringRedisTemplate[]{ stringRedisTemplate1, stringRedisTemplate2 };
	}

	public RedisTemplate<Object, Object> getRedisTemplateW1() {
		return redisTemplateW1;
	}
	public RedisTemplate<Object, Object> getRedisTemplateW2() {
		return redisTemplateW2;
	}
	public StringRedisTemplate getStringRedisTemplate1() {
		return stringRedisTemplate1;
	}
	public StringRedisTemplate getStringRedisTemplate2() {
		return stringRedisTemplate2;
	}

	public void setKeys () {
		redisTemplateReadArray[0].opsForValue().set(key1, key1);
		redisTemplateReadArray[1].opsForValue().set(key2, key2);
	}
	public int getRedisIndex() {
		return redisIndex;
	}
	public void setRedisIndex(int index) {
		redisIndex = index;
	}

	@CircuitBreaker(name = "zCircuitBreaker", fallbackMethod = "switchTemplate")
	public Boolean testTheWrite(String stringKey, String stringValue )  {
		redisTemplateReadArray[redisIndex].opsForValue().set(stringKey, stringValue);
		// logger.info ("after call to set the value");
		String returnValue = (String) redisTemplateReadArray[redisIndex].opsForValue().get(stringKey);
		return false;
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
			if (redisIndex == 0) {
				redisIndex = 1;
				logger.info("Failed over from redistemplate1 to redistemplate2 redisIndex is " + redisIndex);
			} else {
				redisIndex=0;
				logger.info("Failed over from redistemplate2 to redistemplate1 redisIndex is " + redisIndex);
			}
			returnFailedOver = true;
		}
		logger.info("Failover is " + returnFailedOver + " redisIndex is " + redisIndex);
		return returnFailedOver;
	}

	public RedisTemplate<Object, Object> getWriteTemplate(){
		// logger.info("in getWriteTemplate with redisIndex " + redisIndex);
		RedisTemplate<Object, Object> newTemplate = redisTemplateWriteArray[redisIndex];
		return newTemplate;
	}

	public StringRedisTemplate getReadTemplate(){
		// logger.info("in getReadTemplate with redisIndex " + redisIndex);
		StringRedisTemplate newStringRedisTemplate = redisTemplateReadArray[redisIndex];
		return newStringRedisTemplate;
	}

}
