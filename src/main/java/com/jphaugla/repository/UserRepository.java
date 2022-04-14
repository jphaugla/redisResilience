package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.domain.User;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;

@Repository

public class UserRepository {
	private static final String KEY = "User";


	final Logger logger = LoggerFactory.getLogger(UserRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private RedisTemplateRepository redisTemplateRepository;

	public UserRepository() {

		logger.info("UserRepository constructor");
	}

	@Retry(name = "backendA")
	public String create(User user) {

		Map<Object, Object> userHash = mapper.convertValue(user, Map.class);
		redisTemplateRepository.getWriteTemplate().opsForHash().putAll(KEY + user.getUsername(), userHash);
		return "Success\n";
	}

	@Retry(name = "backendA")
	public User get(String username) {
		logger.info("in UserRepository.get with username=" + username);
		String fullKey = KEY + username;
		Map<Object, Object> userHash = redisTemplateRepository.getReadTemplate().opsForHash().entries(fullKey);
		User user = mapper.convertValue(userHash, User.class);
		return (user);
	}

	@Retry(name = "backendA")
	//  this is sample code demonstrating removing all the emails for a customer without using redisearch
	public void delete(String username) {
		logger.info("in User.delete with username " + username);
		String fullKey = KEY + username;
		redisTemplateRepository.getReadTemplate().delete(fullKey);
	}


}
