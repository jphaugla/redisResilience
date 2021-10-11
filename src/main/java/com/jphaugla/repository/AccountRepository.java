package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.domain.Account;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;

@Repository
public class AccountRepository {
	private static final String KEY = "Account";

	final Logger logger = LoggerFactory.getLogger(AccountRepository.class);

	@Autowired
	private RedisTemplateRepository redisTemplateRepository;

	ObjectMapper mapper = new ObjectMapper();

	public AccountRepository() {
		logger.info("AccountRepository constructor");
	}
	public void createAll(List<Account> accounts) {
		for (Account account : accounts) {
			create(account);
		}
	}
	@Retry(name = "backendA")
	public String create(Account account) {
		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		Long millis = System.currentTimeMillis();
		Date date = new Date(millis);
		// logger.info("in accountrepository.create with accountID " + account.getAccountNo() + " " + date);
		// logger.info("redis index is " + redisTemplateRepository.getRedisIndex());
		if (account.getCreatedDate() == null) {
			account.setCreatedDate(millis);
			account.setLastUpdated(millis);
		}
		Map<Object, Account> accountHash = mapper.convertValue(account, Map.class);
		redisTemplateRepository.getWriteTemplate().opsForHash().putAll("Account:" + account.getAccountNo(), accountHash);
		// logger.info(String.format("Account with ID %s saved", account.getAccountNo()));
		return "Success\n";
	}

	@Retry(name = "backendA")
	public Account get(String accountId) {
	//	logger.info("in AccountRepository.get with account id=" + accountId);
	//	logger.info("redis index is " + redisTemplateRepository.getRedisIndex());
		String fullKey = "Account:" + accountId;
		Map<Object, Object> accountHash = redisTemplateRepository.getReadTemplate().opsForHash().entries(fullKey);
		// Map<Object, Object> accountHash = redisTemplate1.opsForHash().entries(fullKey);
		Account account = mapper.convertValue(accountHash, Account.class);
		return (account);
	}

}
