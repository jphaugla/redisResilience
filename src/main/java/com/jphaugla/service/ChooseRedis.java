package com.jphaugla.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

public class ChooseRedis {
    private static final Logger logger = LoggerFactory.getLogger(ChooseRedis.class);


    private int redisIndex;
    public ChooseRedis () {
        logger.info("in chooseRedis constructor");
        redisIndex = 0;
    }
    public int getRedisIndex() {
        return redisIndex;
    }
    public void setRedisIndex(int index) {
        redisIndex = index;
    }


}
