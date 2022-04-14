package com.jphaugla.service;

import com.jphaugla.domain.User;
import com.jphaugla.repository.RedisTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.stream.*;

import org.springframework.data.redis.connection.stream.Record;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;


@Service
public class Messaging {
    private static final Logger logger = LoggerFactory.getLogger(BankService.class);
    public final static String STREAMS_KEY = "STREAM:";
    public final static String USER_PREFIX = "USER:";

    @Autowired
    private RedisTemplateRepository redisTemplateRepository;

    public RecordId postSecurityKey(String username, String password) {

        logger.info("in messageProducer.postSecurityKey with username and password " + username + "/" + password);
        String user_key = USER_PREFIX + username;
        String stream_key = STREAMS_KEY + user_key;
        String timestamp = String.valueOf(System.currentTimeMillis());
        User user = new User(username, password, timestamp);
        ObjectRecord<String, User> objectRecord = StreamRecords.newRecord()
                .in(stream_key)
                .ofObject(user)
                .withId(RecordId.autoGenerate());
        Record record = objectRecord;

        RecordId recordId = redisTemplateRepository.getReadTemplate().opsForStream().add(record);

        Long returnval = redisTemplateRepository.getReadTemplate().opsForZSet().remove(user_key, password);


        if (returnval > 0) {
            logger.info("duplicate password used for username " + username);
        }

        return recordId;
    }
    public String getPassword (String username, Integer number_instances) {
        String user_key = USER_PREFIX + username;
        logger.info("in getPassword with user_key " + user_key);
        String return_password = "";
        String highest_timestamp = "0";
        Set<String> passwordSet = redisTemplateRepository.getReadTemplate().opsForZSet().rangeByScore(user_key,number_instances,number_instances);

        List<ObjectRecord<String, User> > objectRecords = getLatestPasswordfromStream(user_key);
        logger.info("in getPassword with password " + passwordSet.toString());
        for (ObjectRecord<String, User> objectRecord : objectRecords) {
            logger.info("Found object " + objectRecord.getId().toString() + objectRecord.getValue() );
            String new_password = objectRecord.getValue().getPassword();
            String new_timestamp = objectRecord.getValue().getTimestamp();
            Boolean contained = passwordSet.contains(new_password);
            //  if this password from the stream is in the set from the zset it means everyone has read it
            if(new_timestamp.compareTo(highest_timestamp)>0 && contained){
                highest_timestamp = new_timestamp;
                return_password = new_password;
                logger.info("next higher object found " + new_password + " " + new_timestamp);
            }
        }

        return return_password;
    }
    public List<ObjectRecord<String, User>> getLatestPasswordfromStream (String user_key) {
        List<ObjectRecord<String, User> > objectRecords = null;
        String stream_key = STREAMS_KEY + user_key;
        logger.info("in getLatestPasswordfromStream with stream_key " + stream_key);
        // Range.Bound<String> lower = Range.from(Range.Bound<String> "+");
        Range<String> range = Range.open("-","+");
        RedisZSetCommands.Limit limit = new RedisZSetCommands.Limit().count(5);
        // .create("+", "-");
        objectRecords = redisTemplateRepository.getReadTemplate().opsForStream()
                .reverseRange(User.class,stream_key,range,limit);
        if (CollectionUtils.isEmpty(objectRecords)) {
            logger.warn("no log message");
        }
        return objectRecords;
    }
}
