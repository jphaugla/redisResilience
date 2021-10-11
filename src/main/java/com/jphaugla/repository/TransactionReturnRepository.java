package com.jphaugla.repository;

import com.jphaugla.domain.Transaction;
import com.jphaugla.domain.TransactionReturn;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
@Repository

public class TransactionReturnRepository{
	private static final String KEY = "TransactionReturn";


	final Logger logger = LoggerFactory.getLogger(TransactionReturnRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private RedisTemplateRepository redisTemplateRepository;

	public TransactionReturnRepository() {

		logger.info("TransactionReturnRepository constructor");
	}

	@Retry(name = "backendA")
	public String create(TransactionReturn transactionReturn) {
		Map<Object, Object> transactionReturnHash = mapper.convertValue(transactionReturn, Map.class);
		redisTemplateRepository.getWriteTemplate().opsForHash().putAll("TransactionReturn:" + transactionReturn.getReasonCode(), transactionReturnHash);
		// logger.info(String.format("TransactionReturn with ID %s saved", transactionReturn.getReasonCode()));
		return "Success\n";
	}
	public String createAll(List<TransactionReturn> transactionReturnList) {
		for (TransactionReturn transactionReturn : transactionReturnList) {
			create(transactionReturn);
		}
		return "Success\n";
	}

	@Retry(name = "backendA")
	public TransactionReturn get(String transactionReturnId) {
		logger.info("in TransactionReturnRepository.get with transactionReturn id=" + transactionReturnId);
		String fullKey = "TransactionReturn:" + transactionReturnId;
		Map<Object, Object> transactionReturnHash = redisTemplateRepository.getReadTemplate().opsForHash().entries(fullKey);
		TransactionReturn transactionReturn = mapper.convertValue(transactionReturnHash, TransactionReturn.class);
		return (transactionReturn);
	}


}
