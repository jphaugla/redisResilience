package com.jphaugla.service;

import com.jphaugla.repository.RedisTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(BankService.class);
    public final static String STREAMS_KEY = "PASSWORD";
    @Autowired
    private RedisTemplateRepository redisTemplateRepository;
    public RecordId postSecurityKey(String username, String password) {

        logger.info("in messageProducer.postSecurityKey with username and password " + username + "/" + password);
        Map<String, String> messageBody = new HashMap<>();

        messageBody.put("username", username);
        messageBody.put("password", password);
        messageBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
        RecordId messageId = redisTemplateRepository.getRedisTemplateW1().opsForStream().add(
                STREAMS_KEY,
                messageBody);
        return messageId;
    }
}
