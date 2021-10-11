package com.jphaugla.repository;

import com.jphaugla.domain.Merchant;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jphaugla.domain.Transaction;
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

public class MerchantRepository{
	private static final String KEY = "Merchant";


	final Logger logger = LoggerFactory.getLogger(com.jphaugla.repository.MerchantRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private RedisTemplateRepository redisTemplateRepository;

	public MerchantRepository() {

		logger.info("MerchantRepository constructor");
	}

	@Retry(name = "backendA")
	public String create(Merchant merchant) {

		Map<Object, Object> merchantHash = mapper.convertValue(merchant, Map.class);
		redisTemplateRepository.getWriteTemplate().opsForHash().putAll("Merchant:" + merchant.getName(), merchantHash);
		// logger.info(String.format("Merchant with ID %s saved", merchant.getName()));
		return "Success\n";
	}

	@Retry(name = "backendA")
	public Merchant get(String merchantId) {
		// logger.info("in MerchantRepository.get with merchant id=" + merchantId);
		String fullKey = "Merchant:" + merchantId;
		Map<Object, Object> merchantHash = redisTemplateRepository.getReadTemplate().opsForHash().entries(fullKey);
		Merchant merchant = mapper.convertValue(merchantHash, Merchant.class);
		return (merchant);
	}

	public String createAll(List<Merchant> merchantList) {
		for (Merchant merchant : merchantList) {
			create(merchant);
		}
		return "Success\n";
	}


}

